-- BARNAGUER offline booking idempotency.
-- Apply this migration to Supabase before enabling automatic offline synchronization.
-- Repeated POST requests with the same client_request_id are treated as the same request.
ALTER TABLE public.bookings
    ADD COLUMN IF NOT EXISTS client_request_id uuid;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'bookings_client_request_id_key'
          AND conrelid = 'public.bookings'::regclass
    ) THEN
        ALTER TABLE public.bookings
            ADD CONSTRAINT bookings_client_request_id_key UNIQUE (client_request_id);
    END IF;
END
$$;
