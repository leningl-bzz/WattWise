import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

declare var Chart: any;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  sidebarOpen = false;
  activeTab = 'verbrauch';
  dataPoints: any[] = [];

  ngOnInit() {
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/chart.js';
    document.body.appendChild(script);
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebarIfOpen(event: MouseEvent) {
    if (this.sidebarOpen) this.sidebarOpen = false;
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  handleFileChange() {
    // Added handle file change
  }

  processFiles() {
    const sdatInput = document.getElementById('sdatFiles') as HTMLInputElement;
    const eslInput = document.getElementById('eslFiles') as HTMLInputElement;

    const sdatFile = sdatInput?.files?.[0];
    const eslFile = eslInput?.files?.[0];

    if (!sdatFile || !eslFile) {
      alert('Bitte beide Dateien auswählen.');
      return;
    }

    Promise.all([this.readFile(sdatFile), this.readFile(eslFile)])
      .then(([sdatContent, eslContent]) => {
        this.dataPoints = this.mergeAndProcessData(sdatContent, eslContent);
        this.drawCharts(this.dataPoints);
      });
  }

  readFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = e => resolve(e.target?.result as string);
      reader.onerror = e => reject(e);
      reader.readAsText(file);
    });
  }

  mergeAndProcessData(sdatText: string, eslText: string) {
    const sdatLines = sdatText.split('\n').filter(line => line.trim() !== '');
    const eslLines = eslText.split('\n').filter(line => line.trim() !== '');

    const sdatData = sdatLines.map(line => {
      const [timestamp, id, value] = line.split(',');
      return { timestamp, id, value: parseFloat(value) };
    }).filter(d => d.id === '735' || d.id === '742');

    const eslData = eslLines.map(line => {
      const [timestamp, value] = line.split(',');
      return { timestamp, value: parseFloat(value) };
    });

    const combined = sdatData.map(sdat => {
      const esl = eslData.find(e => e.timestamp === sdat.timestamp);
      const zaehlerstand = esl ? esl.value + sdat.value : sdat.value;
      return { timestamp: sdat.timestamp, id: sdat.id, verbrauch: sdat.value, zaehlerstand };
    });

    return combined.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
  }

  drawCharts(data: any[]) {
    const ctx1 = (document.getElementById('verbrauchChart') as HTMLCanvasElement)?.getContext('2d');
    const ctx2 = (document.getElementById('zaehlerstandChart') as HTMLCanvasElement)?.getContext('2d');

    if (!ctx1 || !ctx2 || typeof Chart === 'undefined') {
      console.warn('Chart.js is not loaded yet.');
      return;
    }

    const labels = data.map(d => d.timestamp);
    const verbrauch = data.map(d => d.verbrauch);
    const zaehlerstand = data.map(d => d.zaehlerstand);

    new Chart(ctx1, {
      type: 'line',
      data: {
        labels,
        datasets: [{ label: 'Verbrauch', data: verbrauch, borderColor: 'blue', fill: false }]
      }
    });

    new Chart(ctx2, {
      type: 'line',
      data: {
        labels,
        datasets: [{ label: 'Zählerstand', data: zaehlerstand, borderColor: 'green', fill: false }]
      }
    });
  }

  exportCSV() {
    // Add export logic
  }

  saveJSON() {
    // Add saving logic
  }
}
