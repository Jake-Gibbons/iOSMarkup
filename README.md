# iOSMarkup - Android Image Annotation App

[![Android CI](https://github.com/Jake-Gibbons/iOSMarkup/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Jake-Gibbons/iOSMarkup/actions/workflows/android-ci.yml)
[![Test Coverage](https://github.com/Jake-Gibbons/iOSMarkup/actions/workflows/test-coverage.yml/badge.svg)](https://github.com/Jake-Gibbons/iOSMarkup/actions/workflows/test-coverage.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A feature-rich Android image annotation tool with drawing, shapes, text, and signature capabilities. Built with modern Android architecture patterns and Material 3 design.

## ✨ Features

### Drawing Tools
- **Pen** - Draw with solid lines
- **Marker** - Semi-transparent strokes with blending
- **Eraser** - Tap to remove objects
- **Text** - Add custom text annotations
- **Signature** - Handwritten signature capture

### Shapes
- Rectangle (outline & filled)
- Oval (outline & filled)
- Line
- Arrow

### Advanced Capabilities
- ✅ Undo/Redo functionality
- ✅ Object selection and transformation
- ✅ Multi-touch gestures (pinch-to-zoom, rotate)
- ✅ Canvas zoom and pan
- ✅ Grid overlay (optional)
- ✅ Adjustable stroke width (1-100)
- ✅ State preservation on rotation

### Customization
- 🎨 Customizable color palette with drag-to-reorder
- 🎨 HSV color picker
- 🌓 Light/Dark/System theme modes
- 🎨 Material You dynamic colors (Android 12+)
- 📐 Multiple canvas backgrounds (White, Paper, Dark)

### File Management
- 💾 Save to Pictures or Downloads folder
- 💾 PNG or JPEG export formats
- 📸 Load images from gallery
- 📱 MediaStore integration

## 🏗️ Architecture

Built with modern Android development practices:

- **Pattern:** Repository pattern with MVC approach
- **Language:** 100% Kotlin
- **UI:** XML layouts with Material 3 components
- **Async:** Kotlin Coroutines
- **Storage:** SharedPreferences with Repository abstraction
- **Testing:** JUnit, MockK, Robolectric

### Key Components

- `DrawingView` - Custom view with multi-touch support and transformation
- `SettingsRepository` - Centralized settings management
- `PaletteRepository` - Color palette persistence
- `FileOperations` - Coroutine-based file I/O
- `PermissionHelper` - Cross-version permission handling

## 🧪 Testing

Comprehensive unit test coverage with **105 tests** across critical components:

- `PermissionHelperTest` - 10 tests
- `PaletteRepositoryTest` - 16 tests
- `ValidationTest` - 32 tests
- `FileOperationsTest` - 18 tests
- `SettingsRepositoryTest` - 29 tests

### Running Tests Locally

```bash
# Run all tests
./gradlew test

# Run with coverage report
./gradlew testDebugUnitTest jacocoTestReport

# View HTML report
open app/build/reports/tests/testDebugUnitTest/index.html
```

## 🚀 CI/CD

Automated testing and builds via GitHub Actions:

- ✅ **Unit Tests** - Run on every push and PR
- ✅ **Lint Checks** - Code quality validation
- ✅ **Test Coverage** - Jacoco coverage reports
- ✅ **APK Build** - Automated debug builds
- ✅ **Artifacts** - Test results and APKs uploaded

## 📋 Requirements

- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 34

## 🛠️ Setup

1. Clone the repository:
```bash
git clone https://github.com/Jake-Gibbons/iOSMarkup.git
```

2. Open in Android Studio

3. Sync Gradle dependencies

4. Run on device or emulator

## 📦 Dependencies

### Core
- AndroidX Core KTX 1.12.0
- AppCompat 1.6.1
- Material Components 1.11.0
- ConstraintLayout 2.1.4
- RecyclerView 1.3.2

### Testing
- JUnit 4.13.2
- Kotlinx Coroutines Test 1.7.3
- MockK 1.13.8
- Robolectric 4.11.1

## 🔧 Recent Improvements

### Critical Bug Fixes
- ✅ Fixed memory leaks with `use {}` block for streams
- ✅ Fixed thread safety using synchronized collections
- ✅ Fixed redo stack memory leak in DrawingView
- ✅ Eliminated duplicate palette management

### Code Quality
- ✅ Created PermissionHelper utility for centralized permissions
- ✅ Extracted theme application to SettingsRepository
- ✅ Added comprehensive input validation
- ✅ Documented bitmap ownership with KDoc

### Performance
- ✅ Optimized palette refresh with caching
- ✅ Reduced unnecessary UI recreation

### UX Improvements
- ✅ State preservation on device rotation
- ✅ Replaced Toast with Material 3 Snackbar
- ✅ Added accessibility content descriptions

## 📝 Code Quality

- ✅ Zero magic numbers (all constants extracted)
- ✅ Proper error handling with sealed classes
- ✅ Memory leak prevention with bitmap recycling
- ✅ Type-safe enums and data classes
- ✅ KDoc documentation on public APIs

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Material Design 3 guidelines
- Android Jetpack libraries
- Kotlin Coroutines

---

**Built with ❤️ using Kotlin and Material 3**

🤖 Enhanced with [Claude Code](https://claude.com/claude-code)
