import apiClient from './axios';
export type TicketStatus='OPEN'|'IN_PROGRESS'|'WAITING_FOR_CUSTOMER'|'RESOLVED';
export type TicketCategory='ORDERS'|'PAYMENTS'|'DELIVERY'|'CANCELLATIONS'|'COUPONS'|'REFUNDS'|'OTHER';
export interface SupportMessage{id:number;senderName:string;senderRole:string;body:string;createdAt:string}
export interface SupportTicket{id:number;customerName:string;customerEmail:string;orderId:number|null;category:TicketCategory;subject:string;description:string;status:TicketStatus;assignedAdmin:string|null;createdAt:string;updatedAt:string;messages:SupportMessage[];events:{id:number;actorName:string;fromStatus:TicketStatus|null;toStatus:TicketStatus;note:string;createdAt:string}[]}
export interface FAQ{id:number;category:string;question:string;answer:string;active:boolean;updatedAt:string}
export const supportApi={
 faqs:async(q?:string,category?:string):Promise<FAQ[]> => (await apiClient.get('/support/faqs',{params:{q:q||undefined,category:category||undefined}})).data.data,
 create:async(payload:any):Promise<SupportTicket> => (await apiClient.post('/support/tickets',payload)).data.data,
 tickets:async():Promise<SupportTicket[]> => (await apiClient.get('/support/tickets')).data.data,
 ticket:async(id:number):Promise<SupportTicket> => (await apiClient.get(`/support/tickets/${id}`)).data.data,
 customerReply:async(id:number,message:string):Promise<SupportTicket> => (await apiClient.post(`/support/tickets/${id}/messages`,{message})).data.data,
 adminTickets:async(status?:string,search?:string):Promise<SupportTicket[]> => (await apiClient.get('/admin/support/tickets',{params:{status:status||undefined,search:search||undefined}})).data.data,
 adminReply:async(id:number,message:string):Promise<SupportTicket> => (await apiClient.post(`/admin/support/tickets/${id}/messages`,{message})).data.data,
 setStatus:async(id:number,status:string,note?:string):Promise<SupportTicket> => (await apiClient.patch(`/admin/support/tickets/${id}/status`,{status,note})).data.data,
 assign:async(id:number,adminId:number):Promise<SupportTicket> => (await apiClient.patch(`/admin/support/tickets/${id}/assignment`,{adminId})).data.data,
 admins:async():Promise<{id:number;username:string}[]> => (await apiClient.get('/admin/support/admins')).data.data,
 adminFaqs:async():Promise<FAQ[]> => (await apiClient.get('/admin/support/faqs')).data.data,
 saveFaq:async(id:number|null,payload:any):Promise<FAQ> => (await apiClient[id?'put':'post'](id?`/admin/support/faqs/${id}`:'/admin/support/faqs',payload)).data.data,
 deleteFaq:async(id:number):Promise<void> => {await apiClient.delete(`/admin/support/faqs/${id}`)},
};
