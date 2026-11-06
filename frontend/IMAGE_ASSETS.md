# Local image assets

Food and restaurant photos are stored in `public/images` and served by the frontend from `/images/...`. The photos were downloaded from Unsplash on 2026-10-22 and checked for successful JPEG image responses before being added. Unsplash permits commercial and non-commercial use under its [license](https://unsplash.com/license); attribution is appreciated.

| Local file | Unsplash photo ID | Use |
| --- | --- | --- |
| `restaurant-interior.jpg` | `photo-1414235077428-338989a2e8c0` | Restaurant cards and fallback |
| `indian-thali.jpg` | `photo-1546833999-b9f581a1996d` | Indian thali, hero, biryani |
| `shared-meal.jpg` | `photo-1504674900247-0877df9cc836` | Shared dishes, general fallback |
| `burger.jpg` | `photo-1568901346375-23c9450c58cd` | Burgers |
| `pizza.jpg` | `photo-1565299624946-b28f40a0ae38` | Pizza |
| `noodles.jpg` | `photo-1569718212165-3a8278d5f624` | Noodles, ramen, Chinese dishes |
| `sushi.jpg` | `photo-1579871494447-9811cf80d66c` | Sushi |
| `dosa.jpg` | `photo-1668236543090-82eba5ee5976` | Dosa and South Indian dishes |
| `salad.jpg` | `photo-1512621776951-a57141f2eefd` | Salads and healthy bowls |
| `taco.jpg` | `photo-1551504734-5ee1c4a1479b` | Tacos, wraps, and rolls |
| `dessert.jpg` | `photo-1488477181946-6428a0291777` | Desserts and sweets |
| `curry.jpg` | `photo-1603894584373-5ac82b2ae398` | Indian curries |
| `coffee.jpg` | `photo-1509042239860-f550ce710b93` | Coffee and drinks |

To add a new image, place an optimized JPG, PNG, or WebP file in `public/images`, then save its `/images/filename.ext` path in the admin image path field. External URLs and filesystem paths are rejected by the API.
