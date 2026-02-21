# flyStocks Android Tablet Migration Plan

## Executive Summary

Migrate flyStocks to Android tablets with Bluetooth peripherals. The architecture leverages the existing PWA (flyPush) for the web interface and creates a minimal native Android app (flyPrint) for background printing. Current barcode scanning already works with Bluetooth keyboard-wedge scanners.

**Timeline**: 12 weeks (3 months)
**Cost per setup**: $600-1,125 (printer + tablet + scanner)

---

## Current System Architecture

### flyPush (Web PWA)
- FastAPI backend with multi-tenant architecture
- HTMX + Alpine.js frontend, PWA-capable
- Print job queue via `/api/labels/agent/*` endpoints
- **Barcode scanning works now**: Autofocus search + keyboard-wedge Bluetooth scanners
- Responsive Tailwind CSS design

**Critical Files:**
- `/app/labels/router.py` (lines 712-1007) - Agent API endpoints
- `/app/db/models.py` - PrintJob, PrintAgent models
- `/app/labels/print_service.py` - Print service logic

### flyPrint (Python Agent - Linux/CUPS)
- Polls server every 5s for print jobs
- Uses CUPS to print PDFs/PNGs via USB/network
- Config: `~/.config/flyprint/config.json`
- Runs as systemd service

**Critical Files:**
- `/flyprint/agent.py` - Polling and job processing logic (port to Kotlin)
- `/flyprint/printer.py` - Printer abstraction pattern
- `/flyprint/config.py` - Configuration structure

**Print Job Flow:**
1. User creates job → PENDING status
2. Agent polls `/api/labels/agent/jobs`, claims job → CLAIMED
3. Downloads image from `/api/labels/agent/jobs/{id}/image`
4. Prints → PRINTING status
5. Reports completion → COMPLETED or FAILED

---

## 1. Printer Recommendations

### Winner: Zebra (Production) or Brother (Budget)

| Printer | SDK Quality | Connectivity | Cost | Verdict |
|---------|-------------|--------------|------|---------|
| **Zebra ZD421/ZQ600** | ⭐⭐⭐⭐⭐ Excellent | BT, WiFi, USB | $300-600 | **Best Overall** |
| **Brother QL-820NWB/1110NWB** | ⭐⭐⭐⭐ Very Good | BT, WiFi, USB | $150-350 | **Best Value** |
| **Dymo LabelWriter** | ⭐⭐⭐ Good | WiFi only via proxy | $100-200 | ❌ **Avoid** (needs PC proxy) |

**Zebra Advantages:**
- [Link-OS SDK](https://developer.zebra.com/products/printers/link-os-multiplatform-sdk) with PrintConnect Bluetooth auto-discovery
- Enterprise-grade reliability, ZPL command language
- Wide label support (54mm x 25mm compatible)

**Brother Advantages:**
- [Mobile SDK](https://developerprogram.brother-usa.com/sdk-download) with direct PDF/PNG printing
- Lower cost, good developer docs
- Multiple connectivity options

**Recommendation**: Brother QL-820NWB ($250) for budget-conscious deployments, Zebra ZD421 ($450) for production reliability.

---

## 2. Migration Timeline (12 Weeks)

### Phase 0: Preparation (Week 1)
- [ ] Choose printer brand (Zebra or Brother based on budget)
- [ ] Order hardware: 1 Android tablet ($200-300), 1 printer ($150-600), Bluetooth scanner ($50-100)
- [ ] Set up Android Studio with Kotlin
- [ ] Register for printer SDK (download JAR/AAR)
- [ ] Create Android project structure

### Phase 1: POC - Print Service (Week 2-3)
**Goal**: Prove Android → printer communication works

- [ ] Create minimal Android app with single "Print Test Label" button
- [ ] Integrate printer SDK (discovery, connection, print)
- [ ] Test printing hardcoded QR code PNG (54mm x 25mm)
- [ ] **Deliverable**: App that prints a test label

### Phase 2: POC - API Integration (Week 4)
**Goal**: Prove Android ↔ flyPush API communication

- [ ] Implement Retrofit API client (`FlyPushApi.kt`)
- [ ] Set up OkHttp with API key authentication
- [ ] Manually test: heartbeat, fetch jobs, download PDF/PNG
- [ ] **Deliverable**: App that fetches real jobs from server

### Phase 3: Core Agent Logic (Week 5-6)
**Goal**: Full polling and job processing

- [ ] Implement `FlyPrintService` (foreground service, 5s polling)
- [ ] Implement job processing: claim → download → print → complete
- [ ] Add error handling (network, printer, battery warnings)
- [ ] **Deliverable**: Functional background agent

### Phase 4: Configuration UI (Week 7)
**Goal**: User-friendly setup

- [ ] Build Jetpack Compose setup wizard
- [ ] Implement SharedPreferences storage
- [ ] Add printer discovery UI
- [ ] Connection test before completing setup
- [ ] **Deliverable**: No-code-editing setup experience

### Phase 5: Status & Monitoring (Week 8)
**Goal**: Visibility into agent health

- [ ] Build status dashboard (agent status, recent jobs)
- [ ] Add notifications (job completed/failed, printer error)
- [ ] In-app logs viewer (last 100 lines)
- [ ] **Deliverable**: Observable agent behavior

### Phase 6: PWA Enhancements (Week 9)
**Goal**: Optimize web UI for tablets

- [ ] Add manifest.json and service worker
- [ ] Increase touch target sizes (48dp minimum)
- [ ] Add print agent status widget to dashboard
- [ ] Test on physical tablet (portrait & landscape)
- [ ] **Deliverable**: Installable PWA

### Phase 7: Testing & Polish (Week 10-11)
**Goal**: Ensure reliability

- [ ] Test on multiple Android versions (8.0, 10, 12, 14)
- [ ] Test edge cases (network interruption, printer errors, app kill)
- [ ] Battery life test (8-hour shift simulation)
- [ ] Optimize polling (idle mode when no jobs)
- [ ] **Deliverable**: Production-ready app

### Phase 8: Deployment (Week 12)
**Goal**: Deploy to lab

- [ ] Build release APK (ProGuard, signing)
- [ ] Create installation guide (sideload instructions)
- [ ] Deploy to tablet, configure, pair scanner/printer
- [ ] Train users
- [ ] **Deliverable**: Running system in production

---

## 3. Alternative: Chromebook Support

**Evaluation**: Can we use PWA + Linux container for flyPrint?

**How it works:**
1. Install PWA (flyPush) as normal
2. Enable Linux (Crostini) on Chromebook
3. Install CUPS + existing Python flyPrint in Linux container
4. Connect printer via USB/WiFi

**Limitations:**

| Aspect | Chrome OS | Android Tablet | Winner |
|--------|-----------|----------------|--------|
| Bluetooth Printer | ⚠️ Poor Linux container support | ✅ Native support | **Android** |
| USB Printer | ✅ Works via container | ✅ Via USB OTG | Tie |
| WiFi Printer | ✅ Works well | ✅ Works well | Tie |
| Setup Complexity | ⚠️ Requires Linux knowledge | ✅ Simple app install | **Android** |
| Cost | $$$ ($300-600) | $$ ($150-400) | **Android** |

**Verdict**: **Android tablet is better** due to superior Bluetooth support and simpler UX. Chromebook only viable if printers are WiFi-only.

---

## 4. Cost Breakdown

| Item | Budget Option | Production Option |
|------|--------------|-------------------|
| Android Tablet | $200-300 | $200-300 |
| Printer | Brother QL-820NWB: $250 | Zebra ZD421: $450 |
| Bluetooth Scanner | $50-100 | $50-100 |
| Play Store (optional) | $25 | $25 |
| **Total per setup** | **$600-775** | **$750-1,125** |

---

## 5. Distribution Options

### Option 1: APK Sideload (Recommended for MVP)
- Build release APK, sign with keystore
- Distribute via link/email
- Users enable "Install from Unknown Sources"
- **Pros**: Simple, no approval process, free
- **Cons**: Manual updates

### Option 2: Google Play Store (Internal Testing)
- Create Play Console account ($25 one-time)
- Upload as "Internal Testing" app
- Share with lab members via link
- **Pros**: Auto-updates, professional
- **Cons**: $25 fee, requires Google account

**Recommendation**: Start with APK sideload, migrate to Play Internal Testing for production.

---

## 6. Sources & References

- [Brother Developer SDK](https://developerprogram.brother-usa.com/sdk-download)
- [Zebra Link-OS SDK](https://developer.zebra.com/products/printers/link-os-multiplatform-sdk)
- [Android WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Background Work Best Practices](https://developer.android.com/develop/background-work/background-tasks)
- [Brother Kotlin Extensions](https://github.com/omarmiatello/brother-label-printer-kt)
