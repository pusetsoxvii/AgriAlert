# AgriAlert

Repository prepared for GitHub. The following steps show how to create a remote repository and push this project following best practices.

Quick start (PowerShell):

1. Initialize git, add files and make the first commit:

```powershell
git init
git add .
git commit -m "chore: initial project import"
```

2a. Create a GitHub repo using GitHub CLI (recommended):

```powershell
gh repo create <owner>/AgriAlert --public --source=. --remote=origin --push
```

2b. Or create a new repository on github.com (click New → name it AgriAlert) and then run:

```powershell
git remote add origin https://github.com/<owner>/AgriAlert.git
git branch -M main
git push -u origin main
```

Notes:
- CI: a GitHub Actions workflow at `.github/workflows/android.yml` builds the project on push/PR.
- CONTRIBUTING.md and CODE_OF_CONDUCT.md guide contributors.

**Livestock care · Lesotho**

AgriAlert is a comprehensive mobile application designed to monitor and manage livestock diseases in Lesotho. It facilitates communication between farmers and veterinary officers, enabling rapid response to potential outbreaks and providing a centralized platform for animal health management.

## 🌟 Key Features

### 🚜 For Farmers
- **Report Sick Animals**: Easily report symptoms with detailed descriptions.
- **GPS Integration**: Automatically capture the location of the affected livestock.
- **Photo Evidence**: Upload photos of symptoms directly from the camera or gallery.
- **Track Reports**: Real-time status updates on submitted reports (Pending, Investigating, Resolved).
- **Disease Alerts**: Receive instant notifications about disease outbreaks in your district.
- **Health Library**: Access information about common livestock diseases and prevention.

### 🩺 For Veterinary Officers
- **Dashboard Overview**: Monitor reports specifically within your assigned district.
- **Response System**: Provide advice, schedule visits, or update report statuses.
- **Issue Alerts**: Send urgent disease alerts to all farmers in specific regions.
- **Outbreak Monitoring**: Track trends and severity levels of reported cases.

### 🛡️ For Administrators
- **User Management**: Activate, deactivate, and manage farmer and vet accounts.
- **System Analytics**: View comprehensive statistics on reports, resolved cases, and user growth.
- **Data Oversight**: Full access to all reports and alerts issued across the system.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository Pattern
- **UI Components**: Material Design 3, ViewBinding, SwipeRefreshLayout
- **Database**: Room Persistence Library (SQLite)
- **Concurrency**: Kotlin Coroutines & Lifecycle Scope
- **Image Loading**: Glide
- **Data Visualization**: MPAndroidChart
- **Internationalization**: Support for English and Sesotho

## 📂 Project Structure

- `com.agrialert.app.data`: Contains Room database, Entities, DAOs, and Repository.
- `com.agrialert.app.ui`: Activity and Fragment classes organized by user role.
- `com.agrialert.app.ui.adapter`: Universal adapters for RecyclerViews.
- `com.agrialert.app.utils`: Helper classes for sessions, notifications, and location.

---
*AgriAlert v1.0 · Developed at Botho University*