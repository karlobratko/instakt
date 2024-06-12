# Content Sharing Network

Welcome to the Content Sharing Network! This application allows users to store and access their content, currently
focusing on images.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Architecture](#architecture)
- [Setup](#setup)
- [Development](#development)
- [Cleanup](#cleanup)
- [Usage](#usage)

## Features

- User authentication using JWT.
- Store and retrieve images.
- PostgreSQL database for storing user and image metadata.
- SeaweedFS for image storage.
- Pragmatic functional programming approach using Kotlin and ArrowKt.

## Technologies Used

- **Kotlin**: Programming language.
- **Ktor**: Framework for building asynchronous servers and clients.
- **Exposed**: ORM framework for Kotlin.
- **ArrowKt**: Library for functional programming in Kotlin.
- **PostgreSQL**: Relational database for storing metadata.
- **SeaweedFS**: Distributed file system for storing images.

## Architecture

The application is built using the Ktor framework in a pragmatic functional style with the help of the ArrowKt library.
It uses Exposed ORM for database interactions and integrates with PostgreSQL for metadata storage and SeaweedFS for
image storage.

## Setup

Before running the application, ensure that you have the following installed:

- Podman
- Docker Compose (Podman Compose)
- Gradle

Clone the repository:

```sh
git clone https://github.com/karlobratko/instakt
cd instakt
```

## Development

To set up the development environment, follow these steps:

1. Start the development environment using Podman Compose:
    ```sh
    podman compose -f containers/compose.dev.yml up -d
    ```

2. Run the application in development mode using Gradle:
    ```sh
    ./gradlew :app:runDevelopment
    ```

Alternatively, you can use the provided Makefile to simplify these steps:

```sh
make dev
```

## Cleanup

To clean up the environment, use the following commands:

```sh
podman compose -f containers/compose.dev.yml down
podman volume prune -f
```

Or use the Makefile:

```sh
make cleanup
```

## Usage

Once the application is running, you can interact with it through its API. The API supports operations such as user
authentication, image upload, and retrieval. JWT is used for securing endpoints, so make sure to include the JWT token
in the authorization headers of your requests. Examples of endpoint interactions are visible in resources/requests
folder.

## Contributing

Contributions are welcome! Please submit a pull request or open an issue to discuss your ideas.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.