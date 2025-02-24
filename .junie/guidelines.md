# Kotlin Idiomatic Style Guidelines

### 1. Naming Conventions
- Use camelCase for properties, variables, functions, and parameters
- Use PascalCase for classes and interfaces
- Use SCREAMING_SNAKE_CASE for constants
- Prefer meaningful and descriptive names over abbreviations

### 2. Function Design
- Prefer expression bodies for simple functions
```kotlin
// Good
fun double(x: Int) = x * 2

// Avoid if simple
fun double(x: Int) {
    return x * 2
}
```
- Use named arguments for better readability with multiple parameters
```kotlin
createUser(name = "John", age = 25, email = "john@example.com")
```
- Use extension functions deliberately:
   - Make it a member function if it's essential to the class's core functionality
   - Make it an extension function if it's a utility or enhancement
```kotlin
// Good: Essential functionality as member function
class User(val name: String) {
    fun validateCredentials(): Boolean {  // Core functionality
        // validation logic
    }
}

// Good: Utility functionality as extension function
fun User.toDisplayString(): String {  // Enhancement, not core functionality
    return "${name.capitalize()}"
}

// Bad: Core functionality as extension
fun User.validateCredentials(): Boolean {  // Should be a member function
    // validation logic
}

// Bad: Utility as member
class User(val name: String) {
    fun toDisplayString(): String {  // Should be an extension
        return "${name.capitalize()}"
    }
}
```
- Utilize extension functions to extend functionality cleanly
```kotlin
fun String.toTitleCase(): String = ...
```

### 3. Null Safety
- Leverage Kotlin's null safety features
- Use nullable types only when necessary
- Prefer `?.` (safe call) over `!!` (non-null assertion)
- Use Elvis operator `?:` for default values
```kotlin
val length = str?.length ?: 0
```

### 4. Smart Casts and Type Checks
- Use `when` with `is` checks to leverage smart casts
```kotlin
when (obj) {
    is String -> obj.length
    is List<*> -> obj.size
    else -> null
}
```

### 5. Collections
- Use immutable collections by default (List, Set, Map)
- Use sequence for large collections with multiple operations
- Prefer collection operations over loops for better readability
```kotlin
// Good
val adults = people.filter { it.age >= 18 }.map { it.name }

// Avoid
val adults = mutableListOf<String>()
for (person in people) {
    if (person.age >= 18) {
        adults.add(person.name)
    }
}
```

### 6. Classes and Properties
- Use data classes for DTOs and value objects with these rules:
   - Use only immutable properties (`val`, not `var`)
   - Do not use companion objects in data classes
   - Avoid private constructors
   - Validate parameters in the `init{}` block
   - Use data classes strictly as immutable data holders
```kotlin
// Good
data class User(
    val name: String,
    val age: Int,
    val email: String
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(age >= 0) { "Age cannot be negative" }
        require(email.contains("@")) { "Invalid email format" }
    }
}

// Avoid
data class User(
    var name: String,  // Bad: mutable property
    val age: Int
) {
    companion object {  // Bad: companion object in data class
        fun create(name: String) = User(name, 0)
    }
}
```
- Use object for singletons
- Use companion objects for factory methods and static members (in regular classes, not data classes)

### 7. Coroutines
- Use structured concurrency
- Prefer suspending functions over callbacks
- Use appropriate dispatchers
- Handle exceptions properly with try-catch or supervisorScope

### 8. Anti-patterns to Avoid
1. Overuse of `!!` operator
2. Unnecessary use of nullable types
3. Using Java-style static methods instead of companion objects
4. Excessive use of inheritance over composition
5. Not leveraging Kotlin's standard library functions
6. Using var when val would suffice
7. Ignoring coroutine scope and context
8. Using Java-style loops instead of functional operations

### 9. Best Practices
1. Use sealed classes for representing restricted hierarchies
2. Leverage inline functions for high-order functions
3. Use object declarations for singletons
4. Utilize property delegation when appropriate
5. Use scope functions (let, run, with, apply, also) appropriately
6. Write unit tests using Kotlin-specific testing libraries
7. Use backing properties when needed
8. Implement proper exception handling

### 10. Code Organization
1. Group related functionality into packages
2. Keep files focused and single-purpose
3. Use extension functions to organize utility functions
4. Separate business logic from UI/framework code
5. Follow clean architecture principles