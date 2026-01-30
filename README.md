GymTracker (Phone + Wear OS)

A minimal, no-nonsense gym workout tracker built for Android (Wear OS first,Phone next).

This app is designed for people who actually lift, not for step-count screenshots or social feeds.

⸻

🎯 Goal

The goal of this project is to build a fast, distraction-free workout tracker where:
	•	Workouts are defined on the phone
	•	Exercises contain multiple sets
	•	Each set tracks:
	•	Reps
	•	Weight
	•	Rest time
	•	Editing is fast and finger-friendly
	•	Data ownership stays local (no forced accounts, no cloud bloat)

Wear OS integration is planned for workout execution, not configuration.
The Main reason for this phone app , is that adding workouts, set and reps on watch is very cumbersome, 
so doing it on phone and executing the workouts on watch felt like a good balance between speed and ease of use 


⸻

✨ Current Features

Workouts
	•	Create, update, delete workouts
	•	Navigate into workout details

Exercises
	•	Add and delete exercises per workout
	•	Exercises belong strictly to one workout

Sets
	•	Add and delete sets per exercise
	•	Edit reps (+ / −)
	•	Edit weight (+ / −, 2.5 kg steps)
	•	Edit rest time (+ / −, stored in seconds)
	•	Layout designed to avoid shifting when values change

UI / UX
	•	Built with Jetpack Compose (Material 3)
	•	Large touch targets for use during workouts
	•	Clear hierarchy (important numbers stand out)
	•	Bottom bar for creation actions
	•	No accidental taps on destructive actions

⸻

🧠 Architecture
	•	MVVM
	•	UI is stateless
	•	WorkoutViewModel is the single source of truth
	•	Immutable data updates (Compose-safe)
	•	State driven entirely by ViewModel
	•	No UI logic leaking into data layer

⸻

📦 Data Model (Current)

Workout
 └── Exercise
      └── WorkoutSet
           ├── reps
           ├── weight
           └── restSeconds

	•	Rest time is stored in seconds for timer accuracy
	•	UI formats rest time for display   
    

⸻

🚫 Explicit Non-Goals (for now)

This app intentionally does not include:
	•	User accounts / login
	•	Cloud sync
	•	Analytics
	•	Charts
	•	PR detection
	•	Heart rate tracking
	•	Fancy animations
	•	Social features

These are polish features, not core functionality.

⸻

🔮 Planned Features
	•	Mark set as completed
	•	Automatic rest countdown after set completion
	•	Lock editing during active workout
	•	Wear OS companion app:
	•	Start workouts
	•	Adjust reps/weight/rest
	•	Rest timer on watch
	•	Local persistence (Room)
	•	Optional export later

⸻

🛠 Tech Stack
	•	Kotlin
	•	Jetpack Compose
	•	Material 3
	•	Android ViewModel
	•	(Planned) Room
	•	(Planned) Wear OS

⸻

🧪 Project Status

This is an active learning + build project, focused on:
	•	Proper Android architecture
	•	Real-world Compose patterns
	•	Phone ↔ Watch design constraints

The app is intentionally kept simple to ensure correctness before expansion.

⸻

👤 Author

Built by a developer who lifts and got tired of bloated fitness apps.

⸻
