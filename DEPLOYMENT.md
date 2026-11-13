# Foodie Express deployment

The frontend is hosted on Vercel. The Spring Boot API and its MySQL database are hosted on Render.

## Render backend and MySQL

- Connect this GitHub repository to Render and create a Blueprint from `render.yaml`.
- Render uses the root `Dockerfile` to build the Java 17 Spring Boot API.
- The Blueprint defines a private MySQL service and a public API service in Singapore. MySQL is only reachable over Render's private network.
- The API's persistent disk stores restaurant and food uploads under `/var/data`. MySQL has its own persistent disk at `/var/lib/mysql`.
- During Blueprint setup, provide the Razorpay test key ID and secret when prompted. Keep the secret in Render's environment settings; never add it to source control.
- After Vercel gives the frontend its production domain, set `CORS_ALLOWED_ORIGINS` on Render to that exact origin and redeploy the API.

Render's free web services cannot use persistent disks and spin down after inactivity. MySQL plus user uploads therefore need paid Render services/disks for persistent hosting. With the Blueprint's two Starter services and 11 GB of disks, current rates estimate about US$16.75/month before taxes and usage overages. Render's prices can change; verify the [current pricing](https://render.com/pricing) before creating the Blueprint.

## Vercel frontend

- The Vercel project root must be `frontend`.
- Use the Vite preset, `npm run build`, and `dist` as the output directory.
- Set `VITE_API_BASE_URL` to `https://<render-api-domain>/api`.
- `frontend/vercel.json` rewrites client-side routes to `index.html` so direct links work.

## Preserve local data

The checked-in seed data is not a copy of the local MySQL database. Export the local database with `mysqldump`, import it into Render MySQL, and copy the contents of the local `uploads/` directory to the API disk before switching traffic. The local `uploads/` directory is intentionally gitignored.

The local MySQL database has been exported to `tmp/migration/foodie_express.sql` for the future migration. That dump is gitignored and must be imported into Render MySQL separately; it is not included in this repository push. Do not switch production traffic until the database and uploaded images have been imported and verified.

## Production configuration

- The Blueprint generates private MySQL and JWT secrets.
- Razorpay values are entered in Render as secrets. Keep test credentials until the hosted checkout has been verified.
- CORS should allow only the deployed Vercel origin.
- `.env` files, database dumps, and payment secrets must never be committed.
