import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { couponApi } from '../../api/couponApi';
import { formatCurrency } from '../../lib/utils';

const empty = { code: '', type: 'PERCENTAGE', discountValue: '10', minimumOrderAmount: '0', maximumDiscount: '', startsAt: '', expiresAt: '', active: true, usageLimit: '', perUserLimit: '' };

export default function AdminCoupons() {
  const [rows, setRows] = useState<any[]>([]);
  const [form, setForm] = useState<any>(empty);
  const [editing, setEditing] = useState<number | null>(null);
  const [error, setError] = useState('');

  const refresh = () => couponApi.adminList().then(setRows).catch(e => setError(e.response?.data?.message || 'Could not load coupons'));
  useEffect(() => { void refresh(); }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      const payload = { ...form, discountValue: Number(form.discountValue), minimumOrderAmount: Number(form.minimumOrderAmount), maximumDiscount: form.maximumDiscount === '' ? null : Number(form.maximumDiscount), usageLimit: form.usageLimit === '' ? null : Number(form.usageLimit), perUserLimit: form.perUserLimit === '' ? null : Number(form.perUserLimit) };
      if (editing) await couponApi.update(editing, payload); else await couponApi.create(payload);
      setForm(empty); setEditing(null); await refresh();
    } catch (e: any) { setError(e.response?.data?.message || 'Could not save coupon'); }
  };

  const edit = (coupon: any) => {
    setEditing(coupon.id);
    setForm({ ...coupon, discountValue: String(coupon.discountValue), minimumOrderAmount: String(coupon.minimumOrderAmount), maximumDiscount: coupon.maximumDiscount ?? '', usageLimit: coupon.usageLimit ?? '', perUserLimit: coupon.perUserLimit ?? '', startsAt: coupon.startsAt?.slice(0, 16), expiresAt: coupon.expiresAt?.slice(0, 16) });
  };
  const change = (key: string, value: any) => setForm((current: any) => ({ ...current, [key]: value }));
  const offer = (coupon: any) => coupon.type === 'PERCENTAGE' ? `${coupon.discountValue}%` : coupon.type === 'FIXED' ? `${formatCurrency(coupon.discountValue)} off` : 'Free delivery';
  const toggleActive = async (coupon: any) => { await couponApi.setActive(coupon.id, !coupon.active); void refresh(); };

  return <div className="min-h-screen min-w-0 bg-gray-100 p-4 sm:p-5 lg:ml-64 lg:p-8">
    <aside className="fixed left-0 top-0 hidden h-full w-64 space-y-2 bg-gray-900 p-4 text-gray-200 lg:block">
      <h2 className="mb-5 text-lg font-bold text-white">Foodie Express · Admin</h2>
      {['Dashboard|/admin/dashboard', 'Restaurants|/admin/restaurants', 'Food Items|/admin/food-items', 'Orders|/admin/orders', 'Customers|/admin/customers', 'Coupons & Offers|/admin/coupons', 'Support & FAQs|/admin/support'].map(entry => { const [name, path] = entry.split('|'); return <Link key={path} to={path} className={`block rounded-lg px-4 py-3 ${path === '/admin/coupons' ? 'bg-gray-800 text-white' : 'hover:bg-gray-800'}`}>{name}</Link>; })}
    </aside>
    <main className="min-w-0">
      <h1 className="mb-6 text-2xl font-bold sm:text-3xl">Coupons and Offers</h1>
      {error && <p role="alert" className="mb-4 break-words text-red-700">{error}</p>}
      <form onSubmit={submit} className="grid gap-3 rounded-xl bg-white p-4 shadow sm:p-5 lg:grid-cols-3">
        {(['code', 'discountValue', 'minimumOrderAmount', 'maximumDiscount', 'startsAt', 'expiresAt', 'usageLimit', 'perUserLimit'] as const).map(key => {
          const labels: Record<typeof key, string> = { code: 'Code', discountValue: 'Discount value', minimumOrderAmount: 'Minimum order (₹)', maximumDiscount: 'Maximum discount (₹)', startsAt: 'Starts at', expiresAt: 'Expires at', usageLimit: 'Total redemption limit', perUserLimit: 'Per customer limit' };
          return <label key={key} className="min-w-0 text-sm text-gray-700">{labels[key]}<input required={['code', 'discountValue', 'minimumOrderAmount', 'startsAt', 'expiresAt'].includes(key)} type={key === 'startsAt' || key === 'expiresAt' ? 'datetime-local' : key === 'code' ? 'text' : 'number'} min={key === 'discountValue' || key === 'minimumOrderAmount' ? '0' : undefined} value={form[key]} onChange={event => change(key, event.target.value)} className="mt-1 min-h-11 w-full min-w-0 rounded-lg border p-2" /></label>;
        })}
        <label className="min-w-0 text-sm text-gray-700">Coupon type<select value={form.type} onChange={event => change('type', event.target.value)} className="mt-1 min-h-11 w-full rounded-lg border p-2"><option value="PERCENTAGE">Percentage</option><option value="FIXED">Fixed amount</option><option value="FREE_DELIVERY">Free delivery</option></select></label>
        <label className="flex items-center gap-2"><input type="checkbox" checked={form.active} onChange={event => change('active', event.target.checked)} />Active</label>
        <div className="flex flex-wrap gap-2 lg:col-span-3"><button className="min-h-11 rounded-lg bg-orange-600 px-5 py-2 text-white">{editing ? 'Save changes' : 'Create coupon'}</button>{editing && <button type="button" onClick={() => { setEditing(null); setForm(empty); }} className="min-h-11 rounded-lg border px-5">Cancel</button>}</div>
      </form>
      <section className="mt-6 overflow-hidden rounded-xl bg-white shadow">
        {rows.length === 0 ? <p className="p-6 text-center text-gray-500">No coupons yet.</p> : <>
          <div className="hidden overflow-x-auto xl:block"><table className="w-full min-w-[780px] text-sm"><thead><tr className="border-b text-left"><th className="p-3">Code</th><th>Offer</th><th>Validity</th><th>Usage</th><th>Status</th><th>Actions</th></tr></thead><tbody>{rows.map(({ coupon, reservedOrRedeemed, redeemed }) => <tr key={coupon.id} className="border-b"><td className="p-3 font-bold">{coupon.code}</td><td>{offer(coupon)}<div className="text-xs text-gray-500">Min. {formatCurrency(coupon.minimumOrderAmount)}</div></td><td>{new Date(coupon.startsAt).toLocaleDateString()} – {new Date(coupon.expiresAt).toLocaleDateString()}</td><td>{redeemed} redeemed / {reservedOrRedeemed} reserved or redeemed</td><td>{coupon.active ? 'Active' : 'Inactive'}</td><td className="space-x-2 p-2"><button onClick={() => edit(coupon)} className="text-blue-700">Edit</button><button onClick={() => void toggleActive(coupon)} className="text-orange-700">{coupon.active ? 'Deactivate' : 'Activate'}</button></td></tr>)}</tbody></table></div>
          <div className="space-y-3 p-3 xl:hidden">{rows.map(({ coupon, reservedOrRedeemed, redeemed }) => <article key={coupon.id} className="rounded-xl border p-4"><div className="flex items-start justify-between gap-3"><h2 className="break-all text-lg font-bold">{coupon.code}</h2><span className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${coupon.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'}`}>{coupon.active ? 'Active' : 'Inactive'}</span></div><p className="mt-2">{offer(coupon)}</p><p className="text-sm text-gray-500">Minimum order {formatCurrency(coupon.minimumOrderAmount)}</p><dl className="mt-3 grid grid-cols-2 gap-2 text-sm"><div><dt className="text-xs text-gray-500">Valid</dt><dd>{new Date(coupon.startsAt).toLocaleDateString()} – {new Date(coupon.expiresAt).toLocaleDateString()}</dd></div><div><dt className="text-xs text-gray-500">Usage</dt><dd>{redeemed} used · {reservedOrRedeemed} reserved/used</dd></div></dl><div className="mt-3 flex gap-2 border-t pt-3"><button onClick={() => edit(coupon)} className="min-h-11 flex-1 rounded-lg bg-blue-50 px-3 py-2 font-medium text-blue-700">Edit</button><button onClick={() => void toggleActive(coupon)} className="min-h-11 flex-1 rounded-lg bg-orange-50 px-3 py-2 font-medium text-orange-700">{coupon.active ? 'Deactivate' : 'Activate'}</button></div></article>)}</div>
        </>}
      </section>
    </main>
  </div>;
}
