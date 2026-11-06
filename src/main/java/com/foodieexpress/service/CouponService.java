package com.foodieexpress.service;
import com.foodieexpress.dto.*;
import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service public class CouponService {
 private final CouponRepository coupons; private final CouponRedemptionRepository redemptions;
 private final CartRepository carts;
 private static final BigDecimal DELIVERY=new BigDecimal("40.00"), TAX_RATE=new BigDecimal("0.10");
 private static final List<CouponRedemption.Status> ACTIVE=List.of(CouponRedemption.Status.RESERVED,CouponRedemption.Status.REDEEMED);
 public CouponService(CouponRepository c,CouponRedemptionRepository r,CartRepository carts){this.coupons=c;this.redemptions=r;this.carts=carts;}
 public List<Coupon> offers(){ LocalDateTime now=LocalDateTime.now(); return coupons.findAllByOrderByIdDesc().stream().filter(c->c.isActive()&&!now.isBefore(c.getStartsAt())&&now.isBefore(c.getExpiresAt())).toList(); }
 public CouponTotals preview(Customer customer,String code){
  if(code==null||code.isBlank()) throw new RuntimeException("Enter a coupon code");
  Coupon coupon=coupons.findByCodeIgnoreCase(code.trim()).orElseThrow(()->new RuntimeException("Invalid coupon code"));
  Cart cart=carts.findByCustomer(customer).orElseThrow(()->new RuntimeException("Cart not found"));
  BigDecimal subtotal=cart.getItems().stream().map(i->i.getFoodItem().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
  validate(coupon,customer,subtotal,false,null);
  return calculate(coupon,subtotal);
 }
 public CouponTotals calculate(Coupon c,BigDecimal subtotal){
  BigDecimal tax=subtotal.multiply(TAX_RATE).setScale(2,RoundingMode.HALF_UP), fee=subtotal.signum()>0?DELIVERY:BigDecimal.ZERO, discount;
  switch(c.getType()) {
   case PERCENTAGE -> {discount=subtotal.multiply(c.getDiscountValue()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP); if(c.getMaximumDiscount()!=null)discount=discount.min(c.getMaximumDiscount());}
   case FIXED -> discount=c.getDiscountValue().min(subtotal);
   case FREE_DELIVERY -> discount=fee;
   default -> throw new RuntimeException("Unsupported coupon type");
  }
  discount=discount.max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
  return CouponTotals.builder().code(c.getCode()).type(c.getType().name()).subtotal(subtotal).tax(tax).deliveryFee(fee).discount(discount).grandTotal(subtotal.add(tax).add(fee).subtract(discount).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP)).description(description(c)).minimumOrderAmount(c.getMinimumOrderAmount()).validity(new CouponTotals.LocalDateTimeHolder(c.getStartsAt().toString(),c.getExpiresAt().toString())).build();
 }
 @Transactional public void reserve(String code,Customer customer,Order order,BigDecimal subtotal){
  if(code==null||code.isBlank()) return;
  Coupon c=coupons.lockByCode(code.trim()).orElseThrow(()->new RuntimeException("Invalid coupon code"));
  validate(c,customer,subtotal,true,order);
  CouponRedemption redemption=redemptions.findByOrder(order).orElseGet(CouponRedemption::new);
  redemption.setCoupon(c); redemption.setCustomer(customer); redemption.setOrder(order); redemption.setStatus(CouponRedemption.Status.RESERVED); redemptions.save(redemption);
 }
 private void validate(Coupon c,Customer customer,BigDecimal subtotal,boolean lock,Order sameOrder){
  LocalDateTime now=LocalDateTime.now();
  if(!c.isActive()) throw new RuntimeException("This coupon is inactive");
  if(now.isBefore(c.getStartsAt())) throw new RuntimeException("This coupon is not active yet");
  if(!now.isBefore(c.getExpiresAt())) throw new RuntimeException("This coupon has expired");
  if(subtotal.compareTo(c.getMinimumOrderAmount())<0) throw new RuntimeException("Minimum order of ₹"+c.getMinimumOrderAmount()+" required for this coupon");
  long ownReservation=0;
  if(sameOrder!=null){CouponRedemption own=redemptions.findByOrder(sameOrder).orElse(null);if(own!=null&&own.getCoupon().getId().equals(c.getId())&&own.getCustomer().getId().equals(customer.getId())&&ACTIVE.contains(own.getStatus()))ownReservation=1;}
  if(c.getUsageLimit()!=null&&redemptions.countByCouponAndStatusIn(c,ACTIVE)-ownReservation>=c.getUsageLimit()) throw new RuntimeException("This coupon has reached its usage limit");
  if(c.getPerUserLimit()!=null&&redemptions.countByCouponAndCustomerAndStatusIn(c,customer,ACTIVE)-ownReservation>=c.getPerUserLimit()) throw new RuntimeException("You have reached this coupon's usage limit");
 }
 private String description(Coupon c){return switch(c.getType()){case PERCENTAGE->c.getDiscountValue().stripTrailingZeros().toPlainString()+"% off"+(c.getMaximumDiscount()!=null?", up to ₹"+c.getMaximumDiscount():"");case FIXED->"₹"+c.getDiscountValue()+" off";case FREE_DELIVERY->"Free delivery";};}
 @Transactional public Coupon save(Long id,CouponRequest r){ Coupon c=id==null?new Coupon():coupons.findById(id).orElseThrow(()->new RuntimeException("Coupon not found"));
  String code=r.getCode().trim().toUpperCase(Locale.ROOT); coupons.findByCodeIgnoreCase(code).filter(found->!found.getId().equals(c.getId())).ifPresent(x->{throw new RuntimeException("Coupon code already exists");});
  if(!r.getExpiresAt().isAfter(r.getStartsAt()))throw new RuntimeException("Expiry must be after start time");
  if(r.getType()!=Coupon.Type.FREE_DELIVERY&&r.getDiscountValue().signum()<=0)throw new RuntimeException("Discount value must be greater than zero");
  if(r.getType()==Coupon.Type.PERCENTAGE&&r.getDiscountValue().compareTo(new BigDecimal("100"))>0)throw new RuntimeException("Percentage discount cannot exceed 100");
  c.setCode(code);c.setType(r.getType());c.setDiscountValue(r.getDiscountValue());c.setMinimumOrderAmount(r.getMinimumOrderAmount());c.setMaximumDiscount(r.getMaximumDiscount());c.setStartsAt(r.getStartsAt());c.setExpiresAt(r.getExpiresAt());c.setActive(r.getActive()==null||r.getActive());c.setUsageLimit(r.getUsageLimit());c.setPerUserLimit(r.getPerUserLimit());return coupons.save(c);
 }
 @Transactional public Coupon setActive(Long id,boolean active){Coupon c=coupons.findById(id).orElseThrow(()->new RuntimeException("Coupon not found"));c.setActive(active);return coupons.save(c);}
 public List<Map<String,Object>> adminList(){return coupons.findAllByOrderByIdDesc().stream().map(c->Map.<String,Object>of("coupon",c,"reservedOrRedeemed",redemptions.countByCouponAndStatusIn(c,ACTIVE),"redeemed",redemptions.countByCouponAndStatusIn(c,List.of(CouponRedemption.Status.REDEEMED)))).toList();}
 @Transactional public void complete(Order o){redemptions.findByOrder(o).ifPresent(r->{r.setStatus(CouponRedemption.Status.REDEEMED);redemptions.save(r);});}
 @Transactional public void release(Order o){redemptions.findByOrder(o).filter(r->r.getStatus()==CouponRedemption.Status.RESERVED).ifPresent(r->{r.setStatus(CouponRedemption.Status.RELEASED);redemptions.save(r);});}
}
