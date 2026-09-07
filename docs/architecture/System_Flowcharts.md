# SmartFarm Complete System Flowcharts Specification

## 1. User Authentication, Login & Multi-Method Password Recovery Flow

```mermaid
flowchart TD
    classDef startNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef successNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef errorNode fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef actionNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff;

    Start([User opens Login / Forgot Password]):::startNode --> Choice{Select Action?}:::decisionNode
    
    Choice -- 1. Login --> InputCreds[Enter Username/Email + Password]:::actionNode
    InputCreds --> VerifyDB{Verify Credentials in Database}:::decisionNode
    VerifyDB -- Invalid --> ErrLogin[Show Error: Invalid Credentials]:::errorNode
    VerifyDB -- Valid --> CheckStatus{Account Status?}:::decisionNode
    CheckStatus -- DISABLED --> ErrDisabled[Account Disabled: Contact Farm Admin]:::errorNode
    CheckStatus -- PENDING --> ErrPending[Account Awaiting Admin Approval]:::errorNode
    CheckStatus -- ACTIVE --> SuccessLogin[Authenticated: Generate Session & Load Scoped Dashboard]:::successNode

    Choice -- 2. Self Password Reset --> InputEmail[Enter Registered Email]:::actionNode
    InputEmail --> CheckEmailExists{Email Exists in DB?}:::decisionNode
    CheckEmailExists -- No --> ErrNoEmail[HTTP 400: No account found with this email. Contact Administrator]:::errorNode
    CheckEmailExists -- Yes --> GenToken[Generate 1-Hour Cryptographic Token]:::actionNode
    GenToken --> SendMail[Send HTML Reset Email via Gmail SMTP]:::actionNode
    SendMail --> ClickLink[User clicks Secure Link on Frontend]:::actionNode
    ClickLink --> InputNewPass[Enter New Secure Password]:::actionNode
    InputNewPass --> UpdatePass[Save BCrypt Hash & Invalidate Token]:::successNode
    UpdatePass --> LoginRedirect[Redirect to Login with Success Toast]:::successNode

    Choice -- 3. Staff Admin Direct Reset --> AdminModal[Admin opens User Reset Modal]:::actionNode
    AdminModal --> AdminInput[Enter Temporary Password]:::actionNode
    AdminInput --> DirectUpdate[Update BCrypt Hash directly in DB]:::successNode
    DirectUpdate --> NotifyAdmin[Display Success Toast Notification]:::successNode
```

---

## 2. Staff Provisioning, Quota Limits & Status Lifecycle Flow

```mermaid
flowchart TD
    classDef startNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef adminNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef managerNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef errorNode fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef successNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef actionNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff;

    ProvStart([Provision Staff Member]):::startNode --> CheckCreatorRole{Creator Role?}:::decisionNode
    
    CheckCreatorRole -- ADMIN --> AdminSelectRole{Select Target Role}:::decisionNode
    AdminSelectRole -- MANAGER --> CheckMgrQuota{Current Managers >= 2?}:::decisionNode
    CheckMgrQuota -- Yes --> BlockMgr[HTTP 400: Maximum Manager limit reached - Max 2]:::errorNode
    CheckMgrQuota -- No --> InputStaffData[Enter Name, Username, Email, Password]:::actionNode
    
    AdminSelectRole -- SUPERVISOR --> CheckSupQuota1{Current Supervisors >= 10?}:::decisionNode
    CheckSupQuota1 -- Yes --> BlockSup1[HTTP 400: Maximum Supervisor limit reached - Max 10]:::errorNode
    CheckSupQuota1 -- No --> InputStaffData

    CheckCreatorRole -- MANAGER --> CheckMgrPriv{Has CAN_CREATE_SUPERVISORS?}:::decisionNode
    CheckMgrPriv -- No --> BlockMgrPriv[403 Forbidden: Manager not authorized to create supervisors]:::errorNode
    CheckMgrPriv -- Yes --> CheckSupQuota2{Current Supervisors >= 10?}:::decisionNode
    CheckSupQuota2 -- Yes --> BlockSup2[HTTP 400: Maximum Supervisor limit reached - Max 10]:::errorNode
    CheckSupQuota2 -- No --> SetMgrParent[Auto-link manager_id = currentManager.id]:::actionNode
    SetMgrParent --> InputStaffData

    InputStaffData --> ValUnique{Username / Email Unique?}:::decisionNode
    ValUnique -- Duplicate --> ErrDup[HTTP 400: Username or Email already in use]:::errorNode
    ValUnique -- Unique --> HashPass[Hash Password with BCrypt cost=10]:::actionNode
    HashPass --> SetInitialStatus{Creator is Admin?}:::decisionNode
    SetInitialStatus -- Yes --> SetActive[Set status = ACTIVE]:::successNode
    SetInitialStatus -- No / Self-Signup --> SetPending[Set status = PENDING_APPROVAL]:::actionNode
    SetActive --> SaveUser[Save User Entity in Database]:::successNode
    SetPending --> SaveUser
    SaveUser --> SuccessToast[Show Success Toast Notification]:::successNode
```

---

## 3. Hierarchical Access Delegation & Capacity Verification Flow

```mermaid
flowchart TD
    classDef adminNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef managerNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef supervisorNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef errorNode fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef successNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;

    subgraph ADMIN_TIER["👑 Farm Administrator Level"]
        AdminAction[Admin selects Manager]:::adminNode --> AssignCats[Assign Farm Categories]:::adminNode
        AssignCats --> SetMgrPrivs[Configure Manager Privileges: Create Categories, Create Supervisors, View Financials, Manage Budgets]:::adminNode
        SetMgrPrivs --> CheckMgrWorkload{Assigned Categories > 3?}:::decisionNode
        CheckMgrWorkload -- Yes --> ShowWorkloadWarn[Display Capacity Warning: Manager has >3 Sectors]:::errorNode
        CheckMgrWorkload -- No --> SaveMgrConfig[Persist Manager Privileges in user_privileges]:::successNode
        ShowWorkloadWarn --> SaveMgrConfig
    end

    subgraph MANAGER_TIER["👔 Farm Manager Level"]
        MgrAction[Manager selects Supervisor]:::managerNode --> AssignProjs[Select Projects in Manager's Categories]:::managerNode
        AssignProjs --> CheckSupCap{Selected Projects > Supervisor Capacity?}:::decisionNode
        CheckSupCap -- Yes --> BlockSupAssign[Block: Selected projects exceed supervisor capacity]:::errorNode
        CheckSupCap -- No --> SetSupPrivs[Configure Supervisor Privileges: Harvest, Activities, Inventory, Petty Expenses, Sales]:::managerNode
        SetSupPrivs --> SaveSupConfig[Save Project Assignments & Privileges in DB]:::successNode
    end
```

---

## 4. Labor Compliance, Legal Adult Verification & Field Task Assignment Flow

```mermaid
flowchart TD
    classDef startNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef successNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef errorNode fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef actionNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff;

    StartTask([Assign Workers to Field Activity]):::startNode --> CheckAuth{User is Admin, Manager, or Assigned Supervisor?}:::decisionNode
    CheckAuth -- No --> BlockAuth[403 Forbidden: Not authorized to assign labor on this project]:::errorNode
    
    CheckAuth -- Yes --> SelectWorker{Worker registered in Employee Registry?}:::decisionNode
    
    SelectWorker -- No: New Worker --> InputReg[Enter Full Name, Kenyan National ID, Phone Number, Employment Type]:::actionNode
    InputReg --> ValidateID{Valid Kenyan National ID & Adult Age >= 18?}:::decisionNode
    ValidateID -- Invalid / Minor --> RejectMinor[HTTP 400: Valid National ID required. Minors prohibited by labor law.]:::errorNode
    ValidateID -- Valid Adult --> CheckUnique[Check ID uniqueness in DB]:::actionNode
    CheckUnique --> SaveWorker[Save Verified Employee: EMP-xxx with status = ACTIVE]:::successNode
    
    SelectWorker -- Yes: Existing Worker --> CheckActive{Employee status is ACTIVE?}:::decisionNode
    CheckActive -- Inactive --> BlockInactive[HTTP 400: Inactive employee cannot be assigned to tasks]:::errorNode
    CheckActive -- Active --> AddToRoster[Select Employee(s) for Activity]:::actionNode
    SaveWorker --> AddToRoster

    AddToRoster --> InputWorkload[Specify Assignment Date, Hours Worked, Wage Rate, Output Notes]:::actionNode
    AddToRoster --> CalcWages[Calculate Wage Payable = hoursWorked * (dailyRate / 8)]:::actionNode
    CalcWages --> SaveLaborLog[INSERT INTO activity_labor_assignments]:::successNode
    SaveLaborLog --> TaskSuccess[Activity Labor Roster Persisted & Shift Completed]:::successNode
```

---

## 5. Supplier Procurement & Accounts Payable (Debt) Settlement Flow

```mermaid
flowchart TD
    classDef startNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef successNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef errorNode fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef actionNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff;

    SuppStart([Procure Farm Supplies / Settle Debt]):::startNode --> ActionType{Select Operation?}:::decisionNode

    ActionType -- 1. Record Purchase Invoice --> InputPurchase[Enter Supplier ID, Invoice No, Total Amount, Amount Paid, Terms]:::actionNode
    InputPurchase --> CalcPayable[Calculate balance_due = invoice_amount - amount_paid]:::actionNode
    CalcPayable --> SetStatus{amount_paid vs invoice_amount?}:::decisionNode
    SetStatus -- Full Paid --> SetPaid[payment_status = PAID]:::actionNode
    SetStatus -- Partial --> SetPart[payment_status = PARTIAL]:::actionNode
    SetStatus -- Zero Paid --> SetUnpaid[payment_status = UNPAID]:::actionNode
    
    SetPaid --> SavePurchase[Save supplier_purchases record & Restock Inventory]:::successNode
    SetPart --> SavePurchase
    SetUnpaid --> SavePurchase
    
    SavePurchase --> UpdateSupplierAP[UPDATE suppliers SET total_billed += invoice, total_paid += paid, balance_owed = total_billed - total_paid]:::successNode
    UpdateSupplierAP --> NotifyAP[Update Dashboard Accounts Payable Liability KPI]:::successNode

    ActionType -- 2. Settle Supplier Debt --> SelectSupplier[Select Supplier with balance_owed > 0]:::actionNode
    SelectSupplier --> InputPayment[Enter Payment Amount, Payment Mode, Ref Number]:::actionNode
    InputPayment --> DeductDebt[Deduct from Supplier balance_owed & Record Payment Voucher]:::successNode
    DeductDebt --> APUpdated[Supplier Debt Balance Decremented Successfully]:::successNode
```

---

## 6. Customer Credit Governance & Accounts Receivable Settlement Flow

```mermaid
flowchart TD
    classDef startNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef successNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef errorNode fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef actionNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff;

    SaleStart([Record Sale / Customer Debt Settle]):::startNode --> SaleAction{Action Type?}:::decisionNode

    SaleAction -- 1. Record Produce Sale --> CheckCustomer{Attach Customer?}:::decisionNode
    CheckCustomer -- No / Anonymous --> CashOnlySale[Immediate Full Cash Payment Required]:::actionNode
    CheckCustomer -- Yes --> CheckCreditStatus{Customer credit_status == BLOCKED?}:::decisionNode
    CheckCreditStatus -- Yes --> BlockCredit[HTTP 400: Customer credit blocked due to overdue debt. Cash only.]:::errorNode
    CheckCreditStatus -- No --> InputSaleData[Enter Quantity, Unit Price, Amount Paid, Payment Mode]:::actionNode
    
    InputSaleData --> CalcReceivable[Calculate balance_due = total_amount - amount_paid]:::actionNode
    CalcReceivable --> CheckHarvestStock{Available harvest stock >= quantity?}:::decisionNode
    CheckHarvestStock -- No --> ErrStock[HTTP 400: Cannot sell more than harvested quantity]:::errorNode
    CheckHarvestStock -- Yes --> SaveSaleRec[INSERT INTO sales & Deduct Harvest Balance]:::successNode
    
    SaveSaleRec --> UpdateCustomerAR[UPDATE customers SET total_purchases += total, total_paid += paid, outstanding_debt = total_purchases - total_paid]:::successNode
    UpdateCustomerAR --> SaleSuccess[Sale Recorded & Accounts Receivable Ledger Updated]:::successNode

    SaleAction -- 2. Settle Customer Debt --> FindDebtor[Select Debtor Customer with outstanding_debt > 0]:::actionNode
    FindDebtor --> RecordRemittance[Enter Remitted Amount via Cash/M-Pesa/Bank]:::actionNode
    RecordRemittance --> SettleCustomerAR[UPDATE customers SET total_paid += remitted, outstanding_debt -= remitted]:::successNode
    SettleCustomerAR --> ARClearCheck{outstanding_debt <= 0?}:::decisionNode
    ARClearCheck -- Yes --> SetClear[Set credit_status = CLEAR]:::successNode
    ARClearCheck -- No --> SetDebt[Set credit_status = HAS_DEBT]:::successNode
```

---

## 7. Dynamic Scoped Dashboard & Zero-State Engine Flow

```mermaid
flowchart TD
    classDef startNode fill:#0f172a,stroke:#334155,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef adminNode fill:#4f46e5,stroke:#3730a3,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef managerNode fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef supervisorNode fill:#059669,stroke:#047857,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef decisionNode fill:#d97706,stroke:#b45309,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef zeroNode fill:#e11d48,stroke:#be123c,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef renderNode fill:#10b981,stroke:#059669,stroke-width:2px,color:#ffffff;

    DashStart([User opens Dashboard]):::startNode --> CheckRole{Check User Role}:::decisionNode

    CheckRole -- 1. ADMIN --> AdminDash[Fetch Global Enterprise Metrics]:::adminNode
    AdminDash --> RenderAdmin[Render Full Financial KPIs + AP/AR Debt Metrics + Category Budgets + User Quotas]:::renderNode

    CheckRole -- 2. MANAGER --> FetchMgrCats[Fetch Categories in user_assigned_categories]:::managerNode
    FetchMgrCats --> HasCats{Assigned Categories > 0?}:::decisionNode
    HasCats -- 0 Categories --> MgrZero[SUPPRESS ALL STATS & CHARTS: Render 'No Categories Assigned' Hero Action Card]:::zeroNode
    HasCats -- >= 1 Categories --> MgrDash[Fetch Projects in Assigned Categories]:::managerNode
    MgrDash --> RenderMgr[Render Sector Budgets + Category Projects + Supervisor Workloads + Sector AP/AR]:::renderNode

    CheckRole -- 3. SUPERVISOR --> FetchSupProjs[Fetch Projects where supervisor_id = user.id]:::supervisorNode
    FetchSupProjs --> HasProjs{Assigned Projects > 0?}:::decisionNode
    HasProjs -- 0 Projects --> SupZero[SUPPRESS ALL STATS & CHARTS: Render 'No Projects Assigned' Hero Action Card]:::zeroNode
    HasProjs -- >= 1 Projects --> SupDash[Fetch Yields, Field Activity Tasks & Labor Logs for Assigned Projects]:::supervisorNode
    SupDash --> StripFin[STRIP ALL GLOBAL REVENUE, BUDGETS & FINANCIAL DEBT METRICS]:::supervisorNode
    StripFin --> RenderSup[Render Operational Yields, Activity Logs, Worker Rosters & Completion Progress]:::renderNode
```
