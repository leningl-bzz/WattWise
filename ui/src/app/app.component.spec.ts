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
    expect(component.allDataPoints).toEqual([]);
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
      component.closeSidebarIfOpen();
      expect(component.sidebarOpen).toBeFalse();
    });

    it('should not change sidebar state if already closed', () => {
      component.sidebarOpen = false;
      component.closeSidebarIfOpen();
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

  // Testing public API only - private methods are implementation details
  describe('public API', () => {
    it('should have toggleSidebar method', () => {
      expect(component.toggleSidebar).toBeDefined();
    });

    it('should have closeSidebarIfOpen method', () => {
      expect(component.closeSidebarIfOpen).toBeDefined();
    });

    it('should have setActiveTab method', () => {
      expect(component.setActiveTab).toBeDefined();
    });
  });
});
