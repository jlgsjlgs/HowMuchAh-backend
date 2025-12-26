--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: check_user_whitelist(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.check_user_whitelist() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$BEGIN
  -- Check if the email exists in the whitelist
  IF NOT EXISTS (SELECT 1 FROM public.whitelist WHERE email = NEW.email) THEN
    RAISE EXCEPTION 'Your email (%) is not authorized to access this application. Please contact support for access.', NEW.email;
  END IF;
  
  RETURN NEW;
END;$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: expense_splits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense_splits (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    expense_id uuid NOT NULL,
    user_id uuid NOT NULL,
    amount_owed numeric(10,2) NOT NULL,
    is_settled boolean DEFAULT false NOT NULL,
    CONSTRAINT expense_splits_amount_owed_check CHECK ((amount_owed >= (0)::numeric))
);


--
-- Name: expenses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expenses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    group_id uuid NOT NULL,
    description text NOT NULL,
    total_amount numeric(10,2) NOT NULL,
    currency text DEFAULT 'SGD'::text NOT NULL,
    paid_by_user_id uuid NOT NULL,
    category text NOT NULL,
    expense_date date DEFAULT CURRENT_DATE NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    is_settled boolean DEFAULT false NOT NULL,
    CONSTRAINT expenses_total_amount_check CHECK ((total_amount > (0)::numeric))
);


--
-- Name: group_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_members (
    group_id uuid NOT NULL,
    user_id uuid NOT NULL,
    joined_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name text NOT NULL,
    description text,
    owner_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: invitations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    group_id uuid NOT NULL,
    invited_email text NOT NULL,
    invited_by_user_id uuid NOT NULL,
    status text DEFAULT 'PENDING'::text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    CONSTRAINT invitations_status_check CHECK ((status = ANY (ARRAY['PENDING'::text, 'ACCEPTED'::text, 'DECLINED'::text, 'REVOKED'::text])))
);


--
-- Name: settlement_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.settlement_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    group_id uuid NOT NULL,
    settled_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: settlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.settlements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    payer_user_id uuid NOT NULL,
    payee_user_id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    settlement_group_id uuid NOT NULL,
    currency text DEFAULT 'SGD'::text NOT NULL,
    CONSTRAINT different_users CHECK ((payer_user_id <> payee_user_id)),
    CONSTRAINT settlements_amount_check CHECK ((amount > (0)::numeric))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT auth.uid() NOT NULL,
    email text NOT NULL,
    name text NOT NULL
);


--
-- Name: whitelist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.whitelist (
    email text NOT NULL,
    whitelisted_at timestamp with time zone DEFAULT now()
);


--
-- Name: expense_splits expense_splits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT expense_splits_pkey PRIMARY KEY (id);


--
-- Name: expenses expenses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);


--
-- Name: group_members group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_pkey PRIMARY KEY (group_id, user_id);


--
-- Name: groups groups_name_owner_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_name_owner_unique UNIQUE (name, owner_id);


--
-- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: invitations invitations_group_id_invited_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_group_id_invited_email_key UNIQUE (group_id, invited_email);


--
-- Name: invitations invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_pkey PRIMARY KEY (id);


--
-- Name: settlement_groups settlement_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlement_groups
    ADD CONSTRAINT settlement_groups_pkey PRIMARY KEY (id);


--
-- Name: settlements settlements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT settlements_pkey PRIMARY KEY (id);


--
-- Name: expense_splits unique_user_per_expense; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT unique_user_per_expense UNIQUE (expense_id, user_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_uuid_key UNIQUE (id);


--
-- Name: whitelist whitelist_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.whitelist
    ADD CONSTRAINT whitelist_pkey PRIMARY KEY (email);


--
-- Name: idx_expense_splits_expense_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expense_splits_expense_id ON public.expense_splits USING btree (expense_id);


--
-- Name: idx_expense_splits_is_settled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expense_splits_is_settled ON public.expense_splits USING btree (is_settled);


--
-- Name: idx_expense_splits_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expense_splits_user_id ON public.expense_splits USING btree (user_id);


--
-- Name: idx_expenses_expense_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expenses_expense_date ON public.expenses USING btree (expense_date);


--
-- Name: idx_expenses_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expenses_group_id ON public.expenses USING btree (group_id);


--
-- Name: idx_expenses_is_settled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expenses_is_settled ON public.expenses USING btree (is_settled);


--
-- Name: idx_expenses_paid_by_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expenses_paid_by_user_id ON public.expenses USING btree (paid_by_user_id);


--
-- Name: idx_settlement_groups_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settlement_groups_group_id ON public.settlement_groups USING btree (group_id);


--
-- Name: idx_settlements_currency; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settlements_currency ON public.settlements USING btree (currency);


--
-- Name: idx_settlements_payee_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settlements_payee_user_id ON public.settlements USING btree (payee_user_id);


--
-- Name: idx_settlements_payer_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settlements_payer_user_id ON public.settlements USING btree (payer_user_id);


--
-- Name: idx_settlements_settlement_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settlements_settlement_group_id ON public.settlements USING btree (settlement_group_id);


--
-- Name: users check_whitelist_always; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER check_whitelist_always BEFORE INSERT OR UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.check_user_whitelist();


--
-- Name: expense_splits expense_splits_expense_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT expense_splits_expense_id_fkey FOREIGN KEY (expense_id) REFERENCES public.expenses(id) ON DELETE CASCADE;


--
-- Name: expense_splits expense_splits_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_splits
    ADD CONSTRAINT expense_splits_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: expenses expenses_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: expenses expenses_paid_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_paid_by_user_id_fkey FOREIGN KEY (paid_by_user_id) REFERENCES public.users(id);


--
-- Name: group_members group_members_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: group_members group_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: groups groups_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: invitations invitations_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: invitations invitations_invited_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_invited_by_user_id_fkey FOREIGN KEY (invited_by_user_id) REFERENCES public.users(id);


--
-- Name: settlement_groups settlement_groups_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlement_groups
    ADD CONSTRAINT settlement_groups_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: settlements settlements_payee_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT settlements_payee_user_id_fkey FOREIGN KEY (payee_user_id) REFERENCES public.users(id);


--
-- Name: settlements settlements_payer_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT settlements_payer_user_id_fkey FOREIGN KEY (payer_user_id) REFERENCES public.users(id);


--
-- Name: settlements settlements_settlement_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlements
    ADD CONSTRAINT settlements_settlement_group_id_fkey FOREIGN KEY (settlement_group_id) REFERENCES public.settlement_groups(id) ON DELETE CASCADE;


--
-- Name: users users_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: expense_splits; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.expense_splits ENABLE ROW LEVEL SECURITY;

--
-- Name: expenses; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;

--
-- Name: group_members; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;

--
-- Name: groups; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;

--
-- Name: invitations; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.invitations ENABLE ROW LEVEL SECURITY;

--
-- Name: settlement_groups; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.settlement_groups ENABLE ROW LEVEL SECURITY;

--
-- Name: settlements; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.settlements ENABLE ROW LEVEL SECURITY;

--
-- Name: users; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

--
-- Name: whitelist; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.whitelist ENABLE ROW LEVEL SECURITY;