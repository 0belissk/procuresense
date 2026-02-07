import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HomeComponent } from './home.component';
import { ReorderService } from '../services/reorder.service';

class MockReorderService {
  loadDemoData = jasmine.createSpy('loadDemoData').and.returnValue(of({ orgId: 'demo-org-a', importedRows: 27 }));
  getReorders = jasmine.createSpy('getReorders').and.returnValue(of([
    {
      orgId: 'demo-org-a',
      sku: 'SKU-1',
      productName: 'Eco Towels',
      lastPurchaseAt: '2024-04-01T00:00:00Z',
      medianDaysBetween: 7,
      predictedReorderAt: '2024-04-08T00:00:00Z',
      confidence: 0.8
    }
  ]));
  getBundles = jasmine.createSpy('getBundles').and.returnValue(of([
    {
      orgId: 'demo-org-a',
      sku: 'SKU-1',
      relatedSku: 'SKU-2',
      relatedName: 'Safety Gloves',
      coPurchaseCount: 3
    }
  ]));
}

describe('HomeComponent (Dashboard Shell)', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [{ provide: ReorderService, useClass: MockReorderService }]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the dashboard shell', () => {
    expect(component).toBeTruthy();
    expect(component.hasData()).toBeFalse();
  });

  it('should populate demo data when loader is invoked', () => {
    const service = TestBed.inject(ReorderService) as unknown as MockReorderService;
    component.loadDemoData();
    expect(service.loadDemoData).toHaveBeenCalled();
    expect(service.getReorders).toHaveBeenCalled();
    expect(service.getBundles).toHaveBeenCalled();
    expect(component.hasData()).toBeTrue();
    expect(component.reorderRows().length).toBeGreaterThan(0);
    expect(component.selectedRow()?.sku).toBe(component.reorderRows()[0].sku);
    expect(component.bundlesForSelection().length).toBe(1);
  });

  it('should reset data when switching orgs', () => {
    component.loadDemoData();
    component.selectOrg('demo-org-b');
    expect(component.hasData()).toBeFalse();
    expect(component.selectedRow()).toBeNull();
    expect(component.bundlesForSelection().length).toBe(0);
  });

  it('fetches bundles when selecting a different row', () => {
    const service = TestBed.inject(ReorderService) as unknown as MockReorderService;
    component.loadDemoData();
    component.selectRow({
      sku: 'SKU-99',
      name: 'Sample',
      predictedDate: '—',
      daysUntil: 0,
      confidence: 0.5,
      lastPurchase: '—',
      cadence: 'n/a',
      explanation: 'n/a'
    });
    expect(service.getBundles).toHaveBeenCalledTimes(2);
  });
});
