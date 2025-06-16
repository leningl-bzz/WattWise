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
  progress: number | null = null;
  private chartVerbrauch: any;
  private chartZaehlerstand: any;

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
    this.progress = null;
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

    const formData = new FormData();
    formData.append('sdatFiles', sdatFile);
    formData.append('eslFiles', eslFile);

    this.progress = 0;

    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8080/api/files/upload');

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const percent = Math.round((e.loaded / e.total) * 70);
        this.progress = percent;
      }
    };

    xhr.onload = () => {
      if (xhr.status === 200) {
        const body = JSON.parse(xhr.responseText);
        this.dataPoints = body.map((d: any) => ({
          timestamp: d.timestamp,
          verbrauch: d.relative,
          zaehlerstand: d.absolute
        }));
        this.progress = 90;
        this.drawCharts(this.dataPoints);
        this.progress = 100;
      } else {
        console.error('Upload failed', xhr.statusText);
        this.progress = null;
      }
    };

    xhr.onerror = () => {
      console.error('Request error');
      this.progress = null;
    };

    xhr.send(formData);
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

    if (this.chartVerbrauch) {
      this.chartVerbrauch.destroy();
    }
    if (this.chartZaehlerstand) {
      this.chartZaehlerstand.destroy();
    }

    this.chartVerbrauch = new Chart(ctx1, {
      type: 'line',
      data: {
        labels,
        datasets: [{ label: 'Verbrauch', data: verbrauch, borderColor: 'blue', fill: false }]
      }
    });

    this.chartZaehlerstand = new Chart(ctx2, {
      type: 'line',
      data: {
        labels,
        datasets: [{ label: 'Zählerstand', data: zaehlerstand, borderColor: 'green', fill: false }]
      }
    });
  }

  exportCSV() {
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

    fetch('http://localhost:8080/api/files/exportCsv', {
      method: 'POST',
      body: formData
    }).then(res => {
      if (!res.ok) throw new Error('Fehler beim Export');
      return res.text();
    }).then(csv => {
      const blob = new Blob([csv], { type: 'text/csv' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'export.csv';
      link.click();
    }).catch(err => console.error(err));
  }

  saveJSON() {
    if (!this.dataPoints.length) {
      return;
    }
    const blob = new Blob([JSON.stringify(this.dataPoints, null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'data.json';
    link.click();
  }
}
