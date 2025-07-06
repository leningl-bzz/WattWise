import {AfterViewInit, Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HttpClient, HttpClientModule} from '@angular/common/http';
import {Chart, registerables} from 'chart.js';
import {FormsModule} from '@angular/forms';

Chart.register(...registerables);

type MeterModelResponse = {
  allMeterData?: {
    sensorId: string;
    measurements: {
      timestamp: string;
      relative: number;
      absolute: number;
    }[];
  }[];
  message?: string;
};

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, AfterViewInit {
  /** UI state */
  sidebarOpen = false;
  activeTab = 'verbrauch';

  // Properties for custom date range
  customStartDateString: string = ''; // Holds INSEE-MM-DD string from input
  customEndDateString: string = '';   // Holds INSEE-MM-DD string from input
  displayedDateRange: string = '';    // For displaying "dd/mm/yyyy - dd/mm/yyyy"

  /** Chart data */
  allDataPoints: { timestamp: string; id: string; verbrauch: number; zaehlerstand: number }[] = [];

  /** Chart.js instances */
  verbrauchChartInstance: Chart | null = null;
  zaehlerstandChartInstance: Chart | null = null;

  // API URLs as constants
  private readonly uploadApiUrl = 'http://localhost:8080/api/files/upload';
  private readonly loadExistingApiUrl = 'http://localhost:8080/api/files/load-existing';


  constructor(private http: HttpClient) {
  }

  // Angular Lifecycle Hooks
  ngOnInit(): void {
    console.log('ngOnInit: Attempting to load existing files...');
    this.loadExistingFiles();
    // Initialize custom date strings to a reasonable default
    this.setInitialCustomDateRange();
  }

  ngAfterViewInit(): void {
    console.log('ngAfterViewInit: Checking if charts need to be drawn.');
    if (this.allDataPoints.length > 0) {
      this.drawCharts(this.getFilteredDataPoints());
    } else {
      console.log('ngAfterViewInit: No dataPoints available to draw charts initially.');
    }
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

  // Handle changes in custom date inputs
  onCustomDateChange(): void {
    console.log(`onCustomDateChange: Custom date changed: From ${this.customStartDateString} to ${this.customEndDateString}`);
    this.drawCharts(this.getFilteredDataPoints());
    this.updateDisplayedDateRange(); // Update the display label
  }

  // Set initial custom date range based on available data or a default
  private setInitialCustomDateRange(): void {
    if (this.allDataPoints.length > 0) {
      // Find the earliest and latest timestamps in the loaded data
      const sortedData = [...this.allDataPoints].sort((a, b) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );
      const firstTimestamp = sortedData[0].timestamp;
      const lastTimestamp = sortedData[sortedData.length - 1].timestamp;

      this.customStartDateString = this.formatDateToYYYYMMDD(new Date(firstTimestamp));
      this.customEndDateString = this.formatDateToYYYYMMDD(new Date(lastTimestamp));
      console.log(`setInitialCustomDateRange: Set to data range: ${this.customStartDateString} to ${this.customEndDateString}`);
    } else {
      // Default to a reasonable range if no data is loaded yet
      const today = new Date();
      const oneYearAgo = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate());
      this.customStartDateString = this.formatDateToYYYYMMDD(oneYearAgo);
      this.customEndDateString = this.formatDateToYYYYMMDD(today);
      console.log(`setInitialCustomDateRange: No data, set to default: ${this.customStartDateString} to ${this.customEndDateString}`);
    }
    this.updateDisplayedDateRange(); // Update the display label immediately
  }

  // Helper to format Date object to INSEE-MM-DD string for input[type="date"]
  private formatDateToYYYYMMDD(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // Helper to format Date object to dd/mm/yyyy string for display
  private formatDateToDDMMYYYY(date: Date): string {
    return date.toLocaleString('de-CH', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  // Update the displayed date range string
  private updateDisplayedDateRange(): void {
    let start = this.customStartDateString ? new Date(this.customStartDateString) : null;
    let end = this.customEndDateString ? new Date(this.customEndDateString) : null;

    if (start && end && !isNaN(start.getTime()) && !isNaN(end.getTime())) {
      this.displayedDateRange = `${this.formatDateToDDMMYYYY(start)} - ${this.formatDateToDDMMYYYY(end)}`;
    } else {
      this.displayedDateRange = 'Ungültiger Datumsbereich';
    }
  }

  /* ========== MAIN WORKFLOW ============================================= */

  loadExistingFiles(): void {
    console.log('loadExistingFiles: Sending GET request to backend...');
    this.http.get<MeterModelResponse>(this.loadExistingApiUrl)
      .subscribe({
        next: (resp) => {
          console.log('loadExistingFiles: Backend Response:', resp);
          if (resp && resp.allMeterData && resp.allMeterData.length > 0) {
            this.flattenBackendResponse(resp); // Populates allDataPoints
            this.setInitialCustomDateRange(); // Update custom range to match loaded data
            this.drawCharts(this.getFilteredDataPoints()); // Draw with filtered data
          } else {
            console.log('loadExistingFiles: Keine bestehenden gültigen Messdaten auf dem Server gefunden.');
            this.allDataPoints = [];
            this.destroyCharts();
            this.setInitialCustomDateRange(); // Reset custom range if no data
          }
        },
        error: (err) => {
          console.error('loadExistingFiles: Fehler beim Laden bestehender Dateien:', err);
          const backendErrorMessage = err.error?.message || 'Unbekannter Fehler.';
          alert(`Fehler beim Laden bestehender Dateien: ${backendErrorMessage}`);
          this.allDataPoints = [];
          this.destroyCharts();
          this.setInitialCustomDateRange(); // Reset custom range if error
        }
      });
  }


  /** Triggered by Verarbeiten Button */
  processFiles() {
    console.log('processFiles: Upload button clicked.');
    const sdatInput = document.getElementById('sdatFiles') as HTMLInputElement;
    const eslInput = document.getElementById('eslFiles') as HTMLInputElement;

    console.log('processFiles: SDAT files selected:', sdatInput?.files?.length || 0);
    console.log('processFiles: ESL files selected:', eslInput?.files?.length || 0);


    if (!sdatInput?.files?.length || !eslInput?.files?.length) {
      alert('Bitte sowohl SDAT‑ als auch ESL‑Dateien auswählen.');
      console.warn('processFiles: File selection incomplete, returning.');
      return;
    }

    const formData = new FormData();
    Array.from(sdatInput.files).forEach(f => formData.append('sdatFiles', f, f.name));
    Array.from(eslInput.files).forEach(f => formData.append('eslFiles', f, f.name));

    console.log('processFiles: Sending POST request to backend...');
    this.http.post<MeterModelResponse>(this.uploadApiUrl, formData)
      .subscribe({
        next: (resp) => {
          console.log('processFiles: Backend Response for new upload:', resp);
          if (resp && resp.allMeterData && resp.allMeterData.length > 0) {
            this.loadExistingFiles(); // This will re-fetch all data and then redraw charts
          } else {
            const serverMessage = resp?.message || 'Verarbeitung erfolgreich, aber keine gültigen Messdaten gefunden.';
            console.warn('processFiles: Backend returned no valid meter data for new upload:', serverMessage);
            alert(serverMessage);
            this.allDataPoints = [];
            this.destroyCharts();
            this.setInitialCustomDateRange(); // Reset custom range if no data
          }
        },
        error: (err) => {
          console.error('processFiles: Upload/Parsing Fehler:', err);
          const backendErrorMessage = err.error?.message || 'Upload oder Verarbeitung fehlgeschlagen (siehe Konsole).';
          alert(backendErrorMessage);
          this.allDataPoints = [];
          this.destroyCharts();
          this.setInitialCustomDateRange(); // Reset custom range if error
        }
      });
  }

  /** Convert backend JSON into flat array for charts & export */
  private flattenBackendResponse(resp: MeterModelResponse): void {
    const tempFlattenedData: typeof this.allDataPoints = [];

    if (resp.allMeterData) {
      resp.allMeterData.forEach(meter => {
        if (meter.measurements) {
          meter.measurements.forEach(meas => {
            tempFlattenedData.push({
              timestamp: meas.timestamp,
              id: meter.sensorId,
              verbrauch: meas.relative,
              zaehlerstand: meas.absolute
            });
          });
        } else {
          console.warn(`flattenBackendResponse: Meter ${meter.sensorId} has no measurements.`);
        }
      });
    } else {
      console.warn('flattenBackendResponse: Backend response contained no allMeterData.');
    }

    /* sort by timestamp ascending */
    tempFlattenedData.sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    this.allDataPoints = tempFlattenedData;
    console.log('flattenBackendResponse: Flattened allDataPoints:', this.allDataPoints.length, 'points.');
  }

  // Simplified getFilteredDataPoints - always uses custom inputs
  private getFilteredDataPoints(): typeof this.allDataPoints {
    let startDate: Date | null = null;
    let endDate: Date | null = null;

    // Handle cases where no data is loaded yet
    if (this.allDataPoints.length === 0) {
      console.warn('getFilteredDataPoints: No data available for filtering. Returning empty array.');
      return [];
    }

    // Attempt to parse dates from inputs
    startDate = this.customStartDateString ? new Date(this.customStartDateString) : null;
    endDate = this.customEndDateString ? new Date(this.customEndDateString) : null;

    // Fallback/Validation for Start Date
    if (!startDate || isNaN(startDate.getTime())) {
      console.warn('Invalid custom start date. Falling back to earliest data point.');
      startDate = new Date(this.allDataPoints[0].timestamp); // Fallback to earliest data point
    }
    // Fallback/Validation for End Date
    if (!endDate || isNaN(endDate.getTime())) {
      console.warn('Invalid custom end date. Falling back to latest data point.');
      endDate = new Date(this.allDataPoints[this.allDataPoints.length - 1].timestamp); // Fallback to latest data point
    }

    // Ensure end date is at the very end of the selected day for accurate filtering
    endDate!.setHours(23, 59, 59, 999);
    // Ensure start date is at the very beginning of the selected day
    startDate!.setHours(0, 0, 0, 0);

    // Swap if start date is accidentally after end date
    if (startDate!.getTime() > endDate!.getTime()) {
      console.warn('Custom start date is after end date. Swapping them for filtering.');
      [startDate, endDate] = [endDate, startDate];
    }

    console.log(`getFilteredDataPoints: Filtering from ${startDate!.toLocaleString('de-CH')} to ${endDate!.toLocaleString('de-CH')}.`);

    const filteredData = this.allDataPoints.filter(dp => {
      const dpDate = new Date(dp.timestamp);
      // Ensure date is valid and falls within the selected range
      return !isNaN(dpDate.getTime()) && dpDate >= startDate! && dpDate <= endDate!;
    });

    console.log('getFilteredDataPoints: Filtered data points count:', filteredData.length);
    return filteredData;
  }


  /* ========== CHART RENDERING =========================================== */

  private drawCharts(data: typeof this.allDataPoints) {
    console.log('drawCharts: Attempting to draw charts with', data.length, 'data points (filtered).');

    const verbrauchCanvas = document.getElementById('verbrauchChart') as HTMLCanvasElement;
    const zaehlerstandCanvas = document.getElementById('zaehlerstandChart') as HTMLCanvasElement;

    if (!verbrauchCanvas || !zaehlerstandCanvas) {
      console.error('drawCharts: One or both canvas elements not found in the DOM.');
      console.log('Verbrauch Canvas:', verbrauchCanvas);
      console.log('Zählerstand Canvas:', zaehlerstandCanvas);
      return;
    }

    const ctx1 = verbrauchCanvas.getContext('2d');
    const ctx2 = zaehlerstandCanvas.getContext('2d');

    if (!ctx1 || !ctx2) {
      console.error('drawCharts: Could not get 2D context from one or both canvases.');
      console.log('Verbrauch Context:', ctx1);
      console.log('Zählerstand Context:', ctx2);
      return;
    }

    this.destroyCharts();
    console.log('drawCharts: Old chart instances destroyed.');

    if (data.length === 0) {
      console.log('drawCharts: No data to render, charts will remain empty.');
      return;
    }

    // Prepare data for Chart.js
    const labels = data.map(d => {
      const date = new Date(d.timestamp);
      return isNaN(date.getTime()) ? 'Invalid Date' : date.toLocaleString('de-CH', {
        year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
      });
    });
    const verbrauchVals = data.map(d => d.verbrauch);
    const zaehlerVals = data.map(d => d.zaehlerstand);

    console.log('drawCharts: Chart labels length:', labels.length);
    console.log('drawCharts: Verbrauch values length:', verbrauchVals.length);
    console.log('drawCharts: Zählerstand values length:', zaehlerVals.length);


    // Create Verbrauch Chart
    this.verbrauchChartInstance = new Chart(ctx1, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Verbrauch (kWh)',
          data: verbrauchVals,
          borderColor: 'blue',
          fill: false,
          tension: 0.1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            type: 'category',
            title: {display: true, text: 'Zeitpunkt'},
            ticks: {autoSkip: true, maxTicksLimit: 20}
          },
          y: {
            title: {display: true, text: 'Wert (kWh)'},
            beginAtZero: true
          }
        },
        plugins: {
          tooltip: {
            callbacks: {
              title: (context) => context[0].label
            }
          }
        }
      }
    });
    console.log('drawCharts: Verbrauch Chart created.');

    // Create Zählerstand Chart
    this.zaehlerstandChartInstance = new Chart(ctx2, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Zählerstand (kWh)',
          data: zaehlerVals,
          borderColor: 'green',
          fill: false,
          tension: 0.1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            type: 'category',
            title: {display: true, text: 'Zeitpunkt'},
            ticks: {autoSkip: true, maxTicksLimit: 20}
          },
          y: {
            title: {display: true, text: 'Wert (kWh)'},
            beginAtZero: true
          }
        },
        plugins: {
          tooltip: {
            callbacks: {
              title: (context) => context[0].label
            }
          }
        }
      }
    });
    console.log('drawCharts: Zählerstand Chart created.');
  }

  private destroyCharts(): void {
    if (this.verbrauchChartInstance) {
      this.verbrauchChartInstance.destroy();
      this.verbrauchChartInstance = null;
      console.log('destroyCharts: Verbrauch Chart destroyed.');
    }
    if (this.zaehlerstandChartInstance) {
      this.zaehlerstandChartInstance.destroy();
      this.zaehlerstandChartInstance = null;
      console.log('destroyCharts: Zählerstand Chart destroyed.');
    }
  }

  clearCharts(): void {
    console.log('clearCharts: Clearing charts and local data.');
    this.destroyCharts();
    this.allDataPoints = [];
    console.log('clearCharts: Charts geleert. Dateien bleiben auf dem Server gespeichert.');
    this.setInitialCustomDateRange();
  }


  /* ========== EXPORTS ==================================================== */

  /* CSV export */
  exportCSV() {
    const dataToExport = this.getFilteredDataPoints();

    if (!dataToExport.length) {
      alert('Keine Daten zum Exportieren im aktuellen Diagramm-Bereich.');
      return;
    }

    const sortedData = [...dataToExport].sort( // Sort the filtered data
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    const csvRows = ['Timestamp,ID,Verbrauch,Zählerstand'];
    sortedData.forEach(d =>
      csvRows.push(`${d.timestamp},${d.id},${d.verbrauch},${d.zaehlerstand}`)
    );

    const blob = new Blob([csvRows.join('\n')], {type: 'text/csv;charset=utf-8'});
    this.saveBlob(blob, 'export_filtered.csv'); // Changed filename to indicate filtered
  }

  /* JSON export */
  saveJSON() {
    const dataToExport = this.getFilteredDataPoints();

    if (!dataToExport.length) {
      alert('Keine Daten zum Exportieren im aktuellen Diagramm-Bereich.');
      return;
    }

    const sortedData = [...dataToExport].sort( // Sort the filtered data
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    const blob = new Blob([JSON.stringify(sortedData, null, 2)], {type: 'application/json;charset=utf-8'});
    this.saveBlob(blob, 'data_filtered.json'); // Changed filename to indicate filtered
  }

  private saveBlob(blob: Blob, filename: string) {
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }
}
