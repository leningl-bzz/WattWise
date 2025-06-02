import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  standalone: true
})
export class AppComponent implements OnInit {
  title = 'ui';
  sidebarOpen = false;
  dataPoints: any[] = [];

  ngOnInit() {
    // Load Chart.js script
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/chart.js';
    document.body.appendChild(script);
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  processFiles() {
    const sdatFileInput = document.getElementById('sdatFiles') as HTMLInputElement;
    const eslFileInput = document.getElementById('eslFiles') as HTMLInputElement;

    const sdatFiles = sdatFileInput.files?.[0];
    const eslFiles = eslFileInput.files?.[0];

    if (!sdatFiles || !eslFiles) {
      alert('Bitte beide Dateien auswählen.');
      return;
    }

    Promise.all([this.readFile(sdatFiles), this.readFile(eslFiles)])
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
      return {timestamp, id, value: parseFloat(value)};
    }).filter(d => d.id === '735' || d.id === '742');

    const eslData = eslLines.map(line => {
      const [timestamp, value] = line.split(',');
      return {timestamp, value: parseFloat(value)};
    });

    const combined = sdatData.map(sdat => {
      const esl = eslData.find(e => e.timestamp === sdat.timestamp);
      const zaehlerstand = esl ? esl.value + sdat.value : sdat.value;
      return {
        timestamp: sdat.timestamp,
        id: sdat.id,
        verbrauch: sdat.value,
        zaehlerstand
      };
    });

    combined.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
    return combined;
  }

  drawCharts(data: any[]) {
    const ctx1 = (document.getElementById('verbrauchChart') as HTMLCanvasElement).getContext('2d');
    const ctx2 = (document.getElementById('zaehlerstandChart') as HTMLCanvasElement).getContext('2d');

    if (!ctx1 || !ctx2) return;

    const labels = data.map(d => d.timestamp);
    const verbrauch = data.map(d => d.verbrauch);
    const zaehlerstand = data.map(d => d.zaehlerstand);

    // @ts-ignore - Chart is loaded from CDN
    new Chart(ctx1, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Verbrauch',
          data: verbrauch
        }]
      }
    });

    // @ts-ignore - Chart is loaded from CDN
    new Chart(ctx2, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Zählerstand',
          data: zaehlerstand
        }]
      }
    });
  }

  exportCSV() {
    let csv = 'Timestamp,ID,Verbrauch,Zählerstand\n';
    this.dataPoints.forEach(d => {
      csv += `${d.timestamp},${d.id},${d.verbrauch},${d.zaehlerstand}\n`;
    });

    const blob = new Blob([csv], {type: 'text/csv'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'export.csv';
    link.click();
  }

  saveJSON() {
    const json = JSON.stringify(this.dataPoints, null, 2);
    const blob = new Blob([json], {type: 'application/json'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'data.json';
    link.click();
  }

  postJSON() {
    fetch('https://example.com/api/upload', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(this.dataPoints)
    })
      .then(response => {
        if (response.ok) {
          alert('Daten erfolgreich gesendet.');
        } else {
          alert('Fehler beim Senden.');
        }
      })
      .catch(err => alert('Netzwerkfehler: ' + err));
  }
}
