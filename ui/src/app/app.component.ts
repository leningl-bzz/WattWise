import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HttpClient, HttpClientModule} from '@angular/common/http';
import {Chart, registerables} from 'chart.js';

Chart.register(...registerables);

// This type definition now perfectly matches how Spring/Jackson will serialize MeterModel.
// MeterModel has a 'getAllMeterData()' method which returns a Collection<MeterData>.
// Jackson serializes this collection into a JSON array under the key 'allMeterData'.
type MeterModelResponse = {
  allMeterData: {
    sensorId: string;
    // The 'measurements' property in MeterData (getAllMeasurements()) returns a Collection<Measurement>.
    // Jackson will serialize this as 'measurements' containing Measurement objects.
    measurements: {
      timestamp: string;   // ISO date string (from LocalDateTime)
      relative: number;    // From Measurement.getRelative()
      absolute: number;    // From Measurement.getAbsolute()
    }[];
  }[];
  // Other properties of MeterModel, like 'allMeters' (the map), might also be present
  // in the JSON response if they have public getters and are not @JsonIgnored.
  // We only need 'allMeterData' for this frontend logic.
};

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  /** UI state */
  sidebarOpen = false;
  activeTab = 'verbrauch';

  /** Chart data */
  dataPoints: { timestamp: string; id: string; verbrauch: number; zaehlerstand: number }[] = [];

  /** Chart.js instances */
  verbrauchChartInstance: Chart | null = null;
  zaehlerstandChartInstance: Chart | null = null;

  // API URL as a constant
  private readonly apiUrl = 'http://localhost:8080/api/files/upload';


  constructor(private http: HttpClient) {
  }

  /* ========== UI helpers ================================================= */

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebarIfOpen() {
    if (this.sidebarOpen) this.sidebarOpen = false;
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  /* ========== MAIN WORKFLOW ============================================= */

  /** Triggered by your “Verarbeiten/Hochladen” button */
  processFiles() {
    const sdatInput = document.getElementById('sdatFiles') as HTMLInputElement;
    const eslInput = document.getElementById('eslFiles') as HTMLInputElement;

    if (!sdatInput?.files?.length || !eslInput?.files?.length) {
      alert('Bitte sowohl SDAT‑ als auch ESL‑Dateien auswählen.');
      return;
    }

    /* Build FormData: multiple files per key are allowed */
    const formData = new FormData();
    Array.from(sdatInput.files).forEach(f => formData.append('sdatFiles', f, f.name));
    Array.from(eslInput.files).forEach(f => formData.append('eslFiles', f, f.name));

    /* POST to backend */
    this.http.post<MeterModelResponse>(this.apiUrl, formData) // Use the constant API URL
      .subscribe({
        next: (resp) => {
          console.log('Backend Response:', resp); // Log the full response for debugging
          // Check if allMeterData exists and has data before proceeding
          if (resp && resp.allMeterData && resp.allMeterData.length > 0) {
            this.flattenBackendResponse(resp);
            this.drawCharts(this.dataPoints);
            alert('Dateien erfolgreich verarbeitet!'); // Success feedback
          } else {
            console.warn('Backend returned no valid meter data.');
            alert('Verarbeitung erfolgreich, aber keine gültigen Messdaten gefunden.');
          }
        },
        error: (err) => {
          console.error('Upload/Parsing Fehler:', err);
          // Provide more specific error messages if possible (e.g., from backend error body)
          const errorMessage = err.error?.message || 'Upload oder Verarbeitung fehlgeschlagen (siehe Konsole).';
          alert(errorMessage);
        }
      });
  }

  /** Convert backend JSON into flat array for charts & export */
  private flattenBackendResponse(resp: MeterModelResponse) {
    this.dataPoints = [];          // reset

    /* resp.allMeterData is an array (Jackson serialises Collection) */
    resp.allMeterData.forEach(meter => {
      meter.measurements.forEach(meas => {
        this.dataPoints.push({
          timestamp: meas.timestamp,
          id: meter.sensorId,
          verbrauch: meas.relative,
          zaehlerstand: meas.absolute
        });
      });
    });

    /* sort by timestamp ascending */
    this.dataPoints.sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    console.log('Flattened datapoints:', this.dataPoints);
  }

  /* ========== CHART RENDERING =========================================== */

  private drawCharts(data: typeof this.dataPoints) {
    const ctx1 = (document.getElementById('verbrauchChart') as HTMLCanvasElement)?.getContext('2d');
    const ctx2 = (document.getElementById('zaehlerstandChart') as HTMLCanvasElement)?.getContext('2d');

    if (!ctx1 || !ctx2) {
      console.warn('Canvas‑Kontexte nicht gefunden.');
      return;
    }

    /* Destroy old charts (if any) */
    this.verbrauchChartInstance?.destroy();
    this.zaehlerstandChartInstance?.destroy();

    const labels = data.map(d => d.timestamp);
    const verbrauchVals = data.map(d => d.verbrauch);
    const zaehlerVals = data.map(d => d.zaehlerstand);

    this.verbrauchChartInstance = new Chart(ctx1, {
      type: 'line',
      data: {
        labels,
        datasets: [{label: 'Verbrauch (kWh)', data: verbrauchVals, borderColor: 'blue', fill: false}]
      },
      options: { // Add options for better chart display if needed
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            title: {
              display: true,
              text: 'Zeitpunkt'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Wert (kWh)'
            }
          }
        }
      }
    });

    this.zaehlerstandChartInstance = new Chart(ctx2, {
      type: 'line',
      data: {
        labels,
        datasets: [{label: 'Zählerstand (kWh)', data: zaehlerVals, borderColor: 'green', fill: false}]
      },
      options: { // Add options for better chart display if needed
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            title: {
              display: true,
              text: 'Zeitpunkt'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Wert (kWh)'
            }
          }
        }
      }
    });
  }

  /* ========== EXPORTS ==================================================== */

  exportCSV() {
    if (!this.dataPoints.length) {
      alert('Keine Daten zum Exportieren.');
      return;
    }

    const csvRows = ['Timestamp,ID,Verbrauch,Zählerstand'];
    this.dataPoints.forEach(d =>
      csvRows.push(`${d.timestamp},${d.id},${d.verbrauch},${d.zaehlerstand}`)
    );

    const blob = new Blob([csvRows.join('\n')], {type: 'text/csv;charset=utf-8'}); // Add charset
    this.saveBlob(blob, 'export.csv');
  }

  saveJSON() {
    if (!this.dataPoints.length) {
      alert('Keine Daten zum Exportieren.');
      return;
    }

    const blob = new Blob([JSON.stringify(this.dataPoints, null, 2)], {type: 'application/json;charset=utf-8'}); // Add charset
    this.saveBlob(blob, 'data.json');
  }

  private saveBlob(blob: Blob, filename: string) {
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  handleFileChange() {
    // Currently not used, but can be used to update UI with selected file names.
  }

}
