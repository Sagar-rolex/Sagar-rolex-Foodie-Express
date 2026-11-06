import apiClient from './axios';
export interface WishlistItem {id:number;foodItemId:number;name:string;imagePath?:string|null;price:number|null;rating:number|null;restaurantId:number|null;restaurantName:string|null;available:boolean;removed:boolean;savedAt:string}
export const wishlistApi={
 get:async():Promise<WishlistItem[]> => (await apiClient.get('/wishlist')).data.data,
 add:async(id:number):Promise<WishlistItem> => (await apiClient.post(`/wishlist/items/${id}`)).data.data,
 remove:async(id:number):Promise<void> => {await apiClient.delete(`/wishlist/items/${id}`)},
};
