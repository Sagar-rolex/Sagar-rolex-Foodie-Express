import apiClient from './axios';
export interface CouponTotals { code:string; type:string; subtotal:number; tax:number; deliveryFee:number; discount:number; grandTotal:number; description:string; minimumOrderAmount:number }
export const couponApi = {
 validate: async (code:string):Promise<CouponTotals> => (await apiClient.post('/coupons/validate',{code})).data.data,
 offers: async ():Promise<any[]> => (await apiClient.get('/coupons/offers')).data.data,
 adminList: async ():Promise<any[]> => (await apiClient.get('/admin/coupons')).data.data,
 create: async (payload:any) => (await apiClient.post('/admin/coupons',payload)).data.data,
 update: async (id:number,payload:any) => (await apiClient.put(`/admin/coupons/${id}`,payload)).data.data,
 setActive: async (id:number,active:boolean) => (await apiClient.patch(`/admin/coupons/${id}/active`,{active})).data.data,
};
