# Graph Report - FinTrackApp 2  (2026-09-06)

## Corpus Check
- Corpus is ~5,995 words - fits in a single context window. You may not need a graph.

## Summary
- 119 nodes · 292 edges · 14 communities (9 shown, 1 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.8)
- Token cost: 0 input · 60,544 output

## Community Hubs (Navigation)
- Home Screen & Shared UI
- Categories & Profile Screens
- Transactions Screen
- App Entry & Navigation
- Accounts Screen
- Budget Screen & Mock Data
- Core Tech Stack & Theming
- Navigation Destinations
- Prediction Screen
- Build Tooling (README)

## God Nodes (most connected - your core abstractions)
1. `FinTrack (Android Frontend App)` - 25 edges
2. `FakeData` - 21 edges
3. `SectionCard()` - 18 edges
4. `FinTrackApp()` - 13 edges
5. `formatCurrency()` - 13 edges
6. `FinTrackTopBar()` - 13 edges
7. `HomeScreen()` - 13 edges
8. `FinTrackDestination` - 10 edges
9. `CategoriesScreen()` - 10 edges
10. `BudgetScreen()` - 9 edges

## Surprising Connections (you probably didn't know these)
- `FinTrack (Android Frontend App)` --references--> `MainActivity`  [EXTRACTED]
  README.md → app/src/main/java/com/fintrack/app/MainActivity.kt
- `FakeData` --conceptually_related_to--> `Firebase`  [EXTRACTED]
  app/src/main/java/com/fintrack/app/data/FakeData.kt → README.md
- `FakeData` --conceptually_related_to--> `Retrofit/Ktor`  [EXTRACTED]
  app/src/main/java/com/fintrack/app/data/FakeData.kt → README.md
- `FakeData` --conceptually_related_to--> `Room`  [EXTRACTED]
  app/src/main/java/com/fintrack/app/data/FakeData.kt → README.md
- `FinTrack (Android Frontend App)` --references--> `FakeData`  [EXTRACTED]
  README.md → app/src/main/java/com/fintrack/app/data/FakeData.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Bottom Navigation Screens (Inicio, Cuenta, Transacciones, Presupuesto, Predicción)** — app_src_main_java_com_fintrack_app_ui_screens_homescreen_homescreen, app_src_main_java_com_fintrack_app_ui_screens_accountsscreen_accountsscreen, app_src_main_java_com_fintrack_app_ui_screens_transactionsscreen_transactionsscreen, app_src_main_java_com_fintrack_app_ui_screens_budgetscreen_budgetscreen, app_src_main_java_com_fintrack_app_ui_screens_predictionscreen_predictionscreen [EXTRACTED 1.00]
- **Menu Navigation Flow: Profile → Categories → Subcategories** — app_src_main_java_com_fintrack_app_ui_screens_profilescreen_profilescreen, app_src_main_java_com_fintrack_app_ui_screens_categoriesscreen_categoriesscreen, app_src_main_java_com_fintrack_app_ui_screens_subcategoriesscreen_subcategoriesscreen [EXTRACTED 1.00]
- **Screens Sharing CommonComponents UI Kit** — app_src_main_java_com_fintrack_app_ui_components_commoncomponents_commoncomponents, app_src_main_java_com_fintrack_app_ui_screens_homescreen_homescreen, app_src_main_java_com_fintrack_app_ui_screens_accountsscreen_accountsscreen, app_src_main_java_com_fintrack_app_ui_screens_transactionsscreen_transactionsscreen, app_src_main_java_com_fintrack_app_ui_screens_budgetscreen_budgetscreen, app_src_main_java_com_fintrack_app_ui_screens_predictionscreen_predictionscreen, app_src_main_java_com_fintrack_app_ui_screens_categoriesscreen_categoriesscreen, app_src_main_java_com_fintrack_app_ui_screens_subcategoriesscreen_subcategoriesscreen, app_src_main_java_com_fintrack_app_ui_screens_profilescreen_profilescreen [INFERRED 0.85]

## Communities (14 total, 1 thin omitted)

### Community 0 - "Home Screen & Shared UI"
Cohesion: 0.28
Nodes (13): FinTrackDetailTopBar(), FinTrackLogo(), FinTrackTopBar(), formatCurrency(), Modifier, LabeledProgressBar(), QuickAccessCard(), BalanceCard() (+5 more)

### Community 1 - "Categories & Profile Screens"
Cohesion: 0.30
Nodes (12): SpendingCategory, Subcategory, NavigationRowItem(), SectionCard(), CategoriesScreen(), CategoryRow(), Modifier, ProfileScreen() (+4 more)

### Community 2 - "Transactions Screen"
Cohesion: 0.21
Nodes (13): Transaction, TransactionDirection, GASTO, INGRESO, amountColor(), FilterChip(), Modifier, TransactionFilter (+5 more)

### Community 3 - "App Entry & Navigation"
Cohesion: 0.24
Nodes (10): FinTrackRoot(), MainActivity, composableRoute(), FinTrackApp(), FinTrackBottomBar(), NavigationBarItemDefaultsColors(), FinTrackTheme(), Bundle (+2 more)

### Community 4 - "Accounts Screen"
Cohesion: 0.30
Nodes (11): Account, AccountType, CREDITO, EFECTIVO, INVERSION, PRINCIPAL, AccountCard(), AccountsScreen() (+3 more)

### Community 5 - "Budget Screen & Mock Data"
Cohesion: 0.31
Nodes (9): FakeData, BudgetCategoryLimit, UserProfile, BudgetCategoryCard(), BudgetScreen(), Modifier, Firebase, Retrofit/Ktor (+1 more)

### Community 6 - "Core Tech Stack & Theming"
Cohesion: 0.20
Nodes (11): FinTrackNavGraph, CommonComponents (TopBar, SectionCard, progress bars), Color (theme), Theme, Type (theme), FinTrack (Android Frontend App), Jetpack Compose, Kotlin (+3 more)

### Community 7 - "Navigation Destinations"
Cohesion: 0.20
Nodes (9): Accounts, Budget, Categories, FinTrackDestination, Home, Prediction, Profile, Subcategories (+1 more)

### Community 8 - "Prediction Screen"
Cohesion: 0.47
Nodes (7): FuturePrediction, MonthProjection, Modifier, PredictionRow(), PredictionScreen(), ProjectionBarChart(), TrendAnalysisCard()

## Knowledge Gaps
- **28 isolated node(s):** `PRINCIPAL`, `INVERSION`, `CREDITO`, `EFECTIVO`, `INGRESO` (+23 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 35 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `FinTrack (Android Frontend App)` connect `Core Tech Stack & Theming` to `Home Screen & Shared UI`, `Categories & Profile Screens`, `Transactions Screen`, `App Entry & Navigation`, `Accounts Screen`, `Budget Screen & Mock Data`, `Navigation Destinations`, `Prediction Screen`?**
  _High betweenness centrality (0.338) - this node is a cross-community bridge._
- **Why does `FakeData` connect `Budget Screen & Mock Data` to `Home Screen & Shared UI`, `Categories & Profile Screens`, `Transactions Screen`, `Accounts Screen`, `Core Tech Stack & Theming`, `Prediction Screen`?**
  _High betweenness centrality (0.162) - this node is a cross-community bridge._
- **Why does `FinTrackDestination` connect `Navigation Destinations` to `Home Screen & Shared UI`, `Core Tech Stack & Theming`?**
  _High betweenness centrality (0.147) - this node is a cross-community bridge._
- **What connects `PRINCIPAL`, `INVERSION`, `CREDITO` to the rest of the system?**
  _28 weakly-connected nodes found - possible documentation gaps or missing edges._