# WattWise

WattWise is an energy measurement analysis tool that helps users process and visualize electricity consumption and production data.

## Project Overview

WattWise allows users to upload SDAT and ESL files containing energy measurement data, processes this information, and provides a merged view of the data. This tool is particularly useful for monitoring electricity usage and production, such as in solar panel installations.

## Technologies Used

### Backend
- Java 24
- Spring Boot 3.4.5
- Spring Data JPA
- Spring Web
- Lombok

### Frontend
- Angular 19.2.0
- RxJS 7.8.0
- TypeScript 5.7.2

## Setup Instructions

### Prerequisites
- Java 24 or higher
- Node.js and npm
- Maven

### Installation and Setup

1. Clone the repository to your local machine
2. Navigate to the project root directory
3. Run the `start-application.bat` file to start both the backend and frontend:
   - Double-click the `start-application.bat` file, or
   - Open a command prompt in the project directory and run `start-application.bat`

The batch file will:
1. Start the Spring Boot backend server
2. Install the necessary UI dependencies
3. Start the Angular frontend application

### Manual Setup (Alternative)

If you prefer to start the applications manually:

#### Backend
1. Navigate to the `backend` directory
2. Run `mvnw.cmd spring-boot:run`

#### Frontend
1. Navigate to the `ui` directory
2. Run `npm install` to install dependencies
3. Run `npm start` to start the Angular application

## Usage

1. Once both applications are running, open your browser and navigate to `http://localhost:4200`
2. Use the interface to upload your SDAT and ESL files
3. The application will process the files and display the merged energy measurement data
4. Analyze your electricity consumption (Bezug) and production/feed-in (Einspeisung) data

## File Formats

The application accepts two types of files:
- SDAT files: Containing standardized energy measurement data
- ESL files: Containing additional energy measurement information

Both files are processed and merged to provide a comprehensive view of energy measurements.

## Troubleshooting

If you encounter issues with the application startup:

1. Ensure Java 24 is installed and properly configured
2. Verify that Node.js and npm are installed
3. Check that all dependencies are properly installed
4. Make sure no other applications are using ports 8080 (backend) or 4200 (frontend)

## License

[License information]

## Contributors

[Contributor information]