import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { Component } from '@angular/core';

// Mock Chart.js
(window as any).Chart = class {
  constructor() {}
  static register() {}
  update() {}
  destroy() {}
};

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with default values', () => {
    expect(component.sidebarOpen).toBeFalse();
    expect(component.activeTab).toBe('verbrauch');
    expect(component.dataPoints).toEqual([]);
  });

  describe('toggleSidebar', () => {
    it('should toggle sidebarOpen from false to true', () => {
      component.sidebarOpen = false;
      component.toggleSidebar();
      expect(component.sidebarOpen).toBeTrue();
    });

    it('should toggle sidebarOpen from true to false', () => {
      component.sidebarOpen = true;
      component.toggleSidebar();
      expect(component.sidebarOpen).toBeFalse();
    });
  });

  describe('closeSidebarIfOpen', () => {
    it('should close sidebar if open', () => {
      component.sidebarOpen = true;
      component.closeSidebarIfOpen(new MouseEvent('click'));
      expect(component.sidebarOpen).toBeFalse();
    });

    it('should not change sidebar state if already closed', () => {
      component.sidebarOpen = false;
      component.closeSidebarIfOpen(new MouseEvent('click'));
      expect(component.sidebarOpen).toBeFalse();
    });
  });

  describe('setActiveTab', () => {
    it('should set active tab', () => {
      const tab = 'test-tab';
      component.setActiveTab(tab);
      expect(component.activeTab).toBe(tab);
    });
  });

  describe('readFile', () => {
    it('should read file content', async () => {
      const file = new File(['test content'], 'test.txt', { type: 'text/plain' });
      const content = await component.readFile(file);
      expect(content).toBe('test content');
    });
  });

  describe('mergeAndProcessData', () => {
    it('should process and merge data correctly', () => {
      const sdatText = `2023-01-01T00:00:00,735,100\n2023-01-01T01:00:00,742,200`;
      const eslText = `2023-01-01T00:00:00,50\n2023-01-01T01:00:00,100`;

      const result = component.mergeAndProcessData(sdatText, eslText);

      expect(result.length).toBe(2);
      expect(result[0]).toEqual({
        timestamp: '2023-01-01T00:00:00',
        id: '735',
        verbrauch: 100,
        zaehlerstand: 150
      });
      expect(result[1]).toEqual({
        timestamp: '2023-01-01T01:00:00',
        id: '742',
        verbrauch: 200,
        zaehlerstand: 300
      });
    });

    it('should filter out non-735/742 IDs', () => {
      const sdatText = `2023-01-01T00:00:00,735,100\n2023-01-01T01:00:00,999,200`;
      const eslText = `2023-01-01T00:00:00,50`;

      const result = component.mergeAndProcessData(sdatText, eslText);
      expect(result.length).toBe(1);
      expect(result[0].id).toBe('735');
    });
  });

  describe('processFiles', () => {
    let readFileSpy: jasmine.Spy;
    let drawChartsSpy: jasmine.Spy;
    let mergeAndProcessDataSpy: jasmine.Spy;

    beforeEach(() => {
      // Mock the component's methods
      readFileSpy = spyOn(component, 'readFile').and.callFake((file: File) => {
        if (file.name.endsWith('.sdat')) {
          return Promise.resolve('2023-01-01T00:00:00,735,100');
        } else {
          return Promise.resolve('2023-01-01T00:00:00,50');
        }
      });

      drawChartsSpy = spyOn(component, 'drawCharts');
      mergeAndProcessDataSpy = spyOn(component, 'mergeAndProcessData');
    });



    it('should show alert when files are missing', async () => {
      // Mock document.getElementById to return inputs with no files
      spyOn(document, 'getElementById').and.returnValue({
        files: []
      } as unknown as HTMLElement);

      // Mock alert
      const alertSpy = spyOn(window, 'alert');

      // Call the method
      await component.processFiles();

      // Verify the alert was shown
      expect(alertSpy).toHaveBeenCalledWith('Bitte beide Dateien auswählen.');
      expect(readFileSpy).not.toHaveBeenCalled();
      expect(drawChartsSpy).not.toHaveBeenCalled();
    });
  });

  describe('drawCharts', () => {
    it('should handle missing canvas elements', () => {
      // Mock Chart as undefined to test the warning
      const originalChart = (window as any).Chart;
      (window as any).Chart = undefined;

      const consoleWarnSpy = spyOn(console, 'warn');
      component.drawCharts([]);
      expect(consoleWarnSpy).toHaveBeenCalledWith('Chart.js is not loaded yet.');

      // Restore Chart
      (window as any).Chart = originalChart;
    });

    it('should create charts when canvas elements exist', () => {
      // Create test data
      const testData = [{
        timestamp: '2023-01-01T00:00:00',
        id: '735',
        verbrauch: 100,
        zaehlerstand: 100
      }];

      // Create canvas elements
      const verbrauchCanvas = document.createElement('canvas');
      verbrauchCanvas.id = 'verbrauchChart';
      document.body.appendChild(verbrauchCanvas);

      const zaehlerstandCanvas = document.createElement('canvas');
      zaehlerstandCanvas.id = 'zaehlerstandChart';
      document.body.appendChild(zaehlerstandCanvas);

      // Spy on Chart constructor
      const chartSpy = jasmine.createSpy('Chart');
      const originalChart = (window as any).Chart;
      (window as any).Chart = chartSpy;

      // Call the method
      component.drawCharts(testData);

      // Verify charts were created
      expect(chartSpy).toHaveBeenCalledTimes(2);

      // Clean up
      verbrauchCanvas.remove();
      zaehlerstandCanvas.remove();
      (window as any).Chart = originalChart;
    });
  });
});
