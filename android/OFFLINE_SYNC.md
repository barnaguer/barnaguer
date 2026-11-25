# BARNAGUER offline booking sync

The Supabase project migration `offline_booking_idempotency` is applied.

The `public.bookings.client_request_id` UUID column has a UNIQUE constraint
named `bookings_client_request_id_key`. Android offline sync uses this value
as an idempotency key so retrying the same pending request cannot create a
duplicate booking.

Pending requests remain in the Android Room database until the server accepts
them. A pending request is not a confirmed booking.
