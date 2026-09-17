-- Run in Supabase Dashboard > SQL Editor
create extension if not exists pgcrypto;

create table if not exists public.files (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  storage_path text not null unique,
  mime_type text not null default 'application/octet-stream',
  size bigint not null default 0 check (size >= 0 and size <= 52428800),
  created_at timestamptz not null default now()
);

alter table public.files enable row level security;

create policy "files_select_own" on public.files
for select using (auth.uid() = owner_id);

create policy "files_insert_own" on public.files
for insert with check (auth.uid() = owner_id);

create policy "files_update_own" on public.files
for update using (auth.uid() = owner_id)
with check (auth.uid() = owner_id);

create policy "files_delete_own" on public.files
for delete using (auth.uid() = owner_id);

insert into storage.buckets (id, name, public)
values ('documents', 'documents', false)
on conflict (id) do nothing;

create policy "documents_insert_own" on storage.objects
for insert to authenticated
with check (
  bucket_id = 'documents'
  and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "documents_select_own" on storage.objects
for select to authenticated
using (
  bucket_id = 'documents'
  and owner = auth.uid()
);

create policy "documents_update_own" on storage.objects
for update to authenticated
using (
  bucket_id = 'documents'
  and owner = auth.uid()
)
with check (
  bucket_id = 'documents'
  and owner = auth.uid()
);

create policy "documents_delete_own" on storage.objects
for delete to authenticated
using (
  bucket_id = 'documents'
  and owner = auth.uid()
);
