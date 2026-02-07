import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { DemoLoadResponse, ReorderPredictionDto, ReorderService, BundleRecommendationDto } from '../services/reorder.service';
import { IdentityService } from '../services/identity.service';

interface ReorderRow {
  sku: string;
  name: string;
  lastQuantity: number;
  predictedDate: string;
  daysUntil: number;
  confidence: number;
  lastPurchase: string;
  cadence: string;
  explanation: string;
}

interface BundleCard {
  relatedSku: string;
  relatedName: string;
  count: number;
  rationale: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  readonly orgOptions = ['demo-org-a', 'demo-org-b'];
  readonly roleOptions = ['BUYER', 'ADMIN'];

  readonly selectedOrg = signal(this.identityService.identity().orgId);
  readonly selectedRole = signal(this.identityService.identity().role);
  readonly isLoadingDemo = signal(false);
  readonly infoMessage = signal<string | null>('No data loaded yet. Pick an org and click "Load Demo Data".');
  readonly lastLoadedAt = signal<string | null>(null);
  readonly tableLoading = signal(false);
  readonly tableError = signal<string | null>(null);
  readonly bundleLoading = signal(false);
  readonly bundleError = signal<string | null>(null);
  readonly viewMode = signal<'table' | 'chart'>('table');

  private readonly reorderRowsState = signal<ReorderRow[]>([]);
  readonly reorderRows = computed(() => this.reorderRowsState());
  readonly hasData = computed(() => this.reorderRowsState().length > 0);

  readonly selectedSku = signal<string | null>(null);
  readonly selectedRow = computed(() => {
    const sku = this.selectedSku();
    if (!sku) {
      return null;
    }
    return this.reorderRowsState().find(row => row.sku === sku) ?? null;
  });

  private readonly bundleCardsState = signal<BundleCard[]>([]);
  readonly bundlesForSelection = computed(() => this.bundleCardsState());
  readonly maxLastQuantity = computed(() => {
    const rows = this.reorderRowsState();
    if (!rows.length) {
      return 1;
    }
    return Math.max(1, ...rows.map(row => row.lastQuantity ?? 0));
  });
  readonly chartRows = computed(() => this.reorderRowsState().slice(0, 10));

  readonly rowTrack = (_: number, row: ReorderRow) => row.sku;
  readonly bundleTrack = (_: number, bundle: BundleCard) => bundle.relatedSku;

  constructor(private readonly reorderService: ReorderService,
              private readonly identityService: IdentityService) {}

  loadDemoData(): void {
    if (this.isLoadingDemo()) {
      return;
    }
    this.isLoadingDemo.set(true);
    this.beginRefresh(`Loading demo data for ${this.selectedOrg()}…`);
    this.reorderService.loadDemoData(this.selectedOrg(), this.selectedRole()).subscribe({
      next: (response: DemoLoadResponse) => {
        const timestamp = new Date().toLocaleString();
        this.lastLoadedAt.set(timestamp);
        this.infoMessage.set(`Loaded ${response.importedRows} demo rows for ${response.orgId}.`);
        this.fetchReorders();
      },
      error: (err: unknown) => {
        this.infoMessage.set('Unable to load demo data.');
        this.tableError.set(extractError(err));
        this.bundleError.set('Unable to refresh bundle recommendations.');
        this.bundleCardsState.set([]);
      },
      complete: () => {
        this.isLoadingDemo.set(false);
      }
    });
  }

  selectOrg(orgId: string): void {
    if (this.tableLoading()) {
      return;
    }
    if (orgId === this.selectedOrg()) {
      return;
    }
    this.selectedOrg.set(orgId);
    this.identityService.update(orgId, this.selectedRole());
    this.resetData(`Switched to ${orgId}. Load demo data or upload a CSV to continue.`);
  }

  selectRole(role: string): void {
    if (this.isLoadingDemo() || this.tableLoading() || role === this.selectedRole()) {
      return;
    }
    this.selectedRole.set(role);
    this.identityService.update(this.selectedOrg(), role);
    this.infoMessage.set(`Role switched to ${role}. Controls will adapt as features unlock.`);
  }

  selectRow(row: ReorderRow): void {
    this.selectedSku.set(row.sku);
    this.fetchBundles(row.sku);
  }

  isSelected(row: ReorderRow): boolean {
    return this.selectedSku() === row.sku;
  }

  simulateUpload(): void {
    this.resetData('CSV upload UI coming soon. Demo data cleared so you can start from a clean slate.');
  }

  toggleGraphView(mode: 'table' | 'chart'): void {
    this.viewMode.set(mode);
  }

  barHeight(row: ReorderRow): number {
    if (!row.lastQuantity) {
      return 20;
    }
    const ratio = row.lastQuantity / this.maxLastQuantity();
    return Math.max(10, 20 + ratio * 80);
  }

  barColor(row: ReorderRow): string {
    return this.getConfidenceGradient(row.confidence);
  }

  private getConfidenceGradient(value: number): string {
    const normalized = Math.max(0, Math.min(1, value));
    const hue = Math.max(0, Math.min(120, normalized * 120));
    return `hsl(${hue}, 80%, 40%)`;
  }

  private resetData(message: string): void {
    if (this.isLoadingDemo() || this.tableLoading()) {
      return;
    }
    this.beginRefresh(message);
    this.lastLoadedAt.set(null);
  }

  private fetchReorders(): void {
    this.tableLoading.set(true);
    this.tableError.set(null);
    this.bundleCardsState.set([]);
    this.bundleError.set(null);
    this.reorderService.getReorders(this.selectedOrg(), this.selectedRole()).subscribe({
      next: (rows: ReorderPredictionDto[]) => {
        const mapped = rows.map((item) => this.toRow(item));
        this.reorderRowsState.set(mapped);
        this.selectedSku.set(mapped[0]?.sku ?? null);
        if (!mapped.length) {
          this.infoMessage.set('No reorder predictions yet. Upload or load demo data to populate the table.');
        }
        const firstSku = mapped[0]?.sku ?? null;
        if (firstSku) {
          this.fetchBundles(firstSku);
        } else {
          this.bundleCardsState.set([]);
        }
      },
      error: (err: unknown) => {
        this.reorderRowsState.set([]);
        this.selectedSku.set(null);
        this.bundleCardsState.set([]);
        this.tableError.set(extractError(err));
      },
      complete: () => {
        this.tableLoading.set(false);
      }
    });
  }

  private toRow(item: ReorderPredictionDto): ReorderRow {
    const predictedDate = formatDate(item.predictedReorderAt);
    const lastPurchase = formatDate(item.lastPurchaseAt);
    const daysUntil = daysFromNow(item.predictedReorderAt);
    return {
      sku: item.sku,
      name: item.productName,
      lastQuantity: item.lastQuantity,
      predictedDate,
      daysUntil,
      confidence: item.confidence,
      lastPurchase,
      cadence: `${item.medianDaysBetween} day cadence`,
      explanation: item.explanation ?? 'This recommendation follows the cadence shown above.'
    };
  }

  private fetchBundles(sku: string | null): void {
    this.bundleCardsState.set([]);
    this.bundleError.set(null);
    if (!sku) {
      return;
    }
    this.bundleLoading.set(true);
    this.reorderService.getBundles(this.selectedOrg(), this.selectedRole(), sku).subscribe({
      next: (items: BundleRecommendationDto[]) => {
        this.bundleCardsState.set(items.map(toBundleCard));
      },
      error: (err: unknown) => {
        this.bundleError.set(extractError(err));
      },
      complete: () => {
        this.bundleLoading.set(false);
      }
    });
  }

  private beginRefresh(message: string): void {
    this.reorderRowsState.set([]);
    this.selectedSku.set(null);
    this.tableError.set(null);
    this.tableLoading.set(false);
    this.bundleCardsState.set([]);
    this.bundleError.set(null);
    this.bundleLoading.set(false);
    this.infoMessage.set(message);
  }
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  return isNaN(date.getTime()) ? '—' : date.toLocaleDateString();
}

function daysFromNow(iso: string | null | undefined): number {
  if (!iso) {
    return 0;
  }
  const target = new Date(iso).getTime();
  if (isNaN(target)) {
    return 0;
  }
  const diff = target - Date.now();
  return Math.max(0, Math.round(diff / (1000 * 60 * 60 * 24)));
}

function extractError(err: unknown): string {
  if (!err) {
    return 'Unknown error';
  }
  if (err instanceof Error) {
    return err.message;
  }
  if (typeof err === 'string') {
    return err;
  }
  return 'Unexpected error';
}

function toBundleCard(item: BundleRecommendationDto): BundleCard {
  return {
    relatedSku: item.relatedSku,
    relatedName: item.relatedName,
    count: item.coPurchaseCount,
    rationale: item.rationale ?? 'Often ordered together based on past purchases.'
  };
}
