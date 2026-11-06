import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function calculateCartTotals(items: Array<{ foodItem: { price: number }; quantity: number }>) {
  const subtotalPaise = items.reduce((sum, item) => sum + Math.round(Number(item.foodItem.price) * 100) * item.quantity, 0)
  const taxPaise = Math.round(subtotalPaise * 0.10)
  const deliveryPaise = subtotalPaise > 0 ? 4000 : 0
  return {
    subtotal: subtotalPaise / 100,
    tax: taxPaise / 100,
    deliveryFee: deliveryPaise / 100,
    discount: 0,
    grandTotal: Math.max(0, subtotalPaise + taxPaise + deliveryPaise) / 100,
  }
}

export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function getOrderStatusColor(status: string): string {
  switch (status) {
    case 'PLACED':
      return 'bg-blue-100 text-blue-800'
    case 'CONFIRMED':
      return 'bg-green-100 text-green-800'
    case 'PREPARING':
      return 'bg-yellow-100 text-yellow-800'
    case 'OUT_FOR_DELIVERY':
      return 'bg-purple-100 text-purple-800'
    case 'DELIVERED':
      return 'bg-green-100 text-green-800'
    case 'CANCELLED':
      return 'bg-red-100 text-red-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

export function getPaymentStatusColor(status: string): string {
  switch (status) {
    case 'PENDING':
      return 'bg-yellow-100 text-yellow-800'
    case 'PAID':
      return 'bg-green-100 text-green-800'
    case 'FAILED':
      return 'bg-red-100 text-red-800'
    case 'REFUNDED':
      return 'bg-blue-100 text-blue-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}
