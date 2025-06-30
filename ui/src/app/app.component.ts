import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import Chart, { Chart as ChartType } from 'chart.js/auto';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  private chartVerbrauch: Chart | null = null;
  private chartZaehlerstand: Chart | null = null;
  sidebarOpen = false;
  activeTab = 'verbrauch';
  dataPoints: any[] = [];
  progress: number | null = 0;

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebarIfOpen() {
    if (this.sidebarOpen) this.sidebarOpen = false;
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  async processFiles() {
    const sdatInput = document.getElementById('sdatFiles') as HTMLInputElement;
    const eslInput = document.getElementById('eslFiles') as HTMLInputElement;

    const sdatFile = sdatInput?.files?.[0];
    const eslFile = eslInput?.files?.[0];

    if (!sdatFile || !eslFile) {
      alert('Bitte beide Dateien auswählen.');
      return;
    }

    const formData = new FormData();
    formData.append('sdatFiles', sdatFile);
    formData.append('eslFiles', eslFile);

    try {
      const response = await fetch('http://localhost:8080/api/files/upload', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error('Fehler beim Verarbeiten der Dateien');
      }

      const data = await response.json();
      this.dataPoints = data.map((d: any) => ({
        timestamp: d.timestamp,
        verbrauch: d.relative,
        zaehlerstand: d.absolute,
      }));

      this.drawCharts(this.dataPoints);
      console.log('Dateien erfolgreich verarbeitet');
    } catch (err) {
      console.error('Fehler beim Verarbeiten der Dateien', err);
    }
  }

  async exportCSV() {
    const sdatInput = document.getElementById('sdatFiles') as HTMLInputElement;
    const eslInput = document.getElementById('eslFiles') as HTMLInputElement;

    const sdatFile = sdatInput?.files?.[0];
    const eslFile = eslInput?.files?.[0];

    if (!sdatFile || !eslFile) {
      alert('Bitte beide Dateien auswählen.');
      return;
    }

    const formData = new FormData();
    formData.append('sdatFiles', sdatFile);
    formData.append('eslFiles', eslFile);

    try {
      const response = await fetch('http://localhost:8080/api/files/exportCsv', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error('Fehler beim Exportieren der CSV');
      }

      const csv = await response.text();
      const blob = new Blob([csv], { type: 'text/csv' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'export.csv';
      link.click();
    } catch (err) {
      console.error('Fehler beim Exportieren der CSV', err);
    }
  }

  drawCharts(data: any[]) {
    const ctx1 = (document.getElementById('verbrauchChart') as HTMLCanvasElement)?.getContext('2d');
    const ctx2 = (document.getElementById('zaehlerstandChart') as HTMLCanvasElement)?.getContext('2d');

    if (!ctx1 || !ctx2) {
      console.warn('Canvas context not found.');
      return;
    }

    const labels = data.map((d) => d.timestamp);
    const verbrauch = data.map((d) => d.verbrauch);
    const zaehlerstand = data.map((d) => d.zaehlerstand);

    // Alte Charts zerstören, falls vorhanden
    if (this.chartVerbrauch) {
      this.chartVerbrauch.destroy();
      this.chartVerbrauch = null;
    }
    if (this.chartZaehlerstand) {
      this.chartZaehlerstand.destroy();
      this.chartZaehlerstand = null;
    }

    // Neue Charts erstellen
    this.chartVerbrauch = new Chart(ctx1, {
      type: 'line',
      data: {
        labels,
        datasets: [{ label: 'Verbrauch', data: verbrauch, borderColor: 'blue', fill: false }],
      },
    });

    this.chartZaehlerstand = new Chart(ctx2, {
      type: 'line',
      data: {
        labels,
        datasets: [{ label: 'Zählerstand', data: zaehlerstand, borderColor: 'green', fill: false }],
      },
    });
  }


  handleFileChange() {
    // Optional: du kannst hier z.B. den Fortschritt zurücksetzen
    this.progress = null;
  }

  saveJSON() {
    const json = JSON.stringify(this.dataPoints, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'data.json';
    link.click();
  }
}
