# Project Overview

TrialsScore is a motorcycle trials scoring application for Android. Riders compete in multiple sections across multiple loops, accumulating points from 0 (clean) to 5 (failure) per section. Lower total score wins.

**User Documentation**: See `docs/index.md` for complete usage instructions. Reference this when planning features related to user workflows, data import/export, or end-user experience.
**Technical documentation**: 
- `tech-docs/training-pipeline-with-kaggle.md` - ML pipeline
- `tech-docs/trials-card-scanning-approach.md` - camera picture -> card extraction using opencv -> ML to list of scores  

# Module Structure

- **`:app`** - Main Android application (MVVM, Jetpack Compose, Room).
- **`:shared-cv`** - Computer vision components (OpenCV card scanning, row segmentation).
- **`:ml-inference`** - Score classification (TensorFlow Lite inference).
- **`:data-prep-tool`** - Training data preparation. 

Each module has a dedicated README.md with detailed architecture, dependencies, and implementation notes.

# Testing

**Naming conventions**:
- Test methods: `<methodBeingTested>_<expectation>_<optionalConditions>`
- Test fakes: `Fake<InterfaceName>` in same package under `/test/java/`

**Unit tests** (`/test/`): JUnit with Hamcrest, JavaFaker
**Instrumented tests** (`/androidTest/`): Compose testing with `ANDROIDX_TEST_ORCHESTRATOR`
