import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate, Navigate } from 'react-router-dom';
import { billApi } from '../api/billApi';
import type { Bill } from '../types';
import { toast } from 'sonner';
import { formatDate, formatCurrency, getPaymentStatusColor } from '../lib/utils';
import { ArrowLeft, Download, CreditCard } from 'lucide-react';

export default function Bill() {
  const { id } = useParams<{ id: string }>();
  const [bill, setBill] = useState<Bill | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchBill = async () => {
      if (!id) return;
      try {
        const response = await billApi.getBillById(Number(id));
        setBill(response.data);
      } catch (error: any) {
        if (error.response?.status === 401) {
          toast.error('Please login to view bill');
          navigate('/login');
        } else {
          toast.error('Failed to load bill');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchBill();
  }, [id, navigate]);

  const handleDownloadInvoice = () => {
    // Create a printable invoice
    if (!bill) return;

    const order = bill.order;
    const invoiceContent = `
FOODIE EXPRESS
==============
INVOICE

Order ID: #${order.id}
Date: ${formatDate(bill.generatedAt)}

Customer: ${order.customer.fullName}
Restaurant: ${order.restaurant.name}
Delivery Address: ${order.deliveryAddress}

--------------------------------------------
ITEMS
--------------------------------------------
${order.items.map(item => `${item.foodItem.name.padEnd(30)} ${String(item.quantity).padEnd(5)} ${formatCurrency(item.priceAtOrder)}`).join('\n')}
--------------------------------------------

Subtotal: ${formatCurrency(bill.totalAmount)}
Tax: ${formatCurrency(bill.tax)}
Delivery: ${formatCurrency(bill.deliveryFee || 0)}
${bill.couponCode ? `Coupon (${bill.couponCode}): -${formatCurrency(bill.discount || 0)}` : ''}

TOTAL: ${formatCurrency(bill.grandTotal)}

Payment Status: ${bill.paymentStatus}

Thank you for ordering with Foodie Express!
    `.trim();

    const blob = new Blob([invoiceContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `invoice-order-${order.id}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    toast.success('Invoice downloaded!');
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="bg-gray-200 h-96 rounded-lg animate-pulse" />
      </div>
    );
  }

  if (!bill) {
    return <Navigate to="/orders" replace />;
  }

  const order = bill.order;

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <Link to={`/orders/${order.id}`} className="flex items-center text-gray-600 hover:text-gray-900 mb-6">
        <ArrowLeft size={20} className="mr-2" />
        Back to Order
      </Link>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Invoice Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white p-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold mb-2">🍽️ Foodie Express</h1>
              <p className="text-blue-100">INVOICE</p>
            </div>
            <div className="text-right">
              <p className="text-2xl font-bold">#{order.id}</p>
              <p className="text-blue-100">{formatDate(bill.generatedAt)}</p>
            </div>
          </div>
        </div>

        {/* Customer & Restaurant Info */}
        <div className="p-6 border-b">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h3 className="font-semibold text-gray-900 mb-2">Customer</h3>
              <p className="text-gray-600">{order.customer.fullName}</p>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 mb-2">Restaurant</h3>
              <p className="text-gray-600">{order.restaurant.name}</p>
              <p className="text-gray-500 text-sm">{order.restaurant.address}</p>
            </div>
          </div>
          <div className="mt-4">
            <h3 className="font-semibold text-gray-900 mb-2">Delivery Address</h3>
            <p className="text-gray-600">{order.deliveryAddress}</p>
          </div>
        </div>

        {/* Order Items */}
        <div className="p-6 border-b">
          <h3 className="font-semibold text-gray-900 mb-4">Items</h3>
          <table className="w-full table-fixed">
            <thead>
              <tr className="text-left text-gray-500 text-sm border-b">
                <th className="w-[55%] pb-2">Item</th>
                <th className="w-[15%] pb-2 text-center">Qty</th>
                <th className="w-[30%] pb-2 text-right">Price</th>
              </tr>
            </thead>
            <tbody>
              {order.items.map((item) => (
                <tr key={item.id} className="border-b last:border-b-0">
                  <td className="break-words py-3 pr-2">{item.itemNameAtOrder || item.foodItem.name}</td>
                  <td className="py-3 text-center">{item.quantity}</td>
                  <td className="py-3 text-right">{formatCurrency(item.priceAtOrder * item.quantity)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Totals */}
        <div className="p-6">
          <div className="space-y-2">
            <div className="flex justify-between text-gray-600">
              <span>Subtotal</span>
              <span>{formatCurrency(bill.totalAmount)}</span>
            </div>
            <div className="flex justify-between text-gray-600">
              <span>Tax</span>
              <span>{formatCurrency(bill.tax)}</span>
            </div>
            <div className="flex justify-between text-gray-600">
              <span>Delivery</span>
              <span>{formatCurrency(bill.deliveryFee || 0)}</span>
            </div>
            {bill.couponCode && <div className="flex justify-between text-green-700"><span>Coupon ({bill.couponCode})</span><span>−{formatCurrency(bill.discount || 0)}</span></div>}
            <hr className="my-3" />
            <div className="flex justify-between text-xl font-bold">
              <span>Total</span>
              <span className="text-blue-600">{formatCurrency(bill.grandTotal)}</span>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <CreditCard size={20} />
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getPaymentStatusColor(bill.paymentStatus)}`}>
                {bill.paymentStatus}
              </span>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="p-6 bg-gray-50 flex flex-col sm:flex-row gap-4">
          <button
            onClick={handleDownloadInvoice}
            className="flex-1 flex items-center justify-center space-x-2 bg-gray-800 text-white px-4 py-3 rounded-lg hover:bg-gray-900 transition"
          >
            <Download size={20} />
            <span>Download Invoice</span>
          </button>

        </div>
      </div>
    </div>
  );
}
