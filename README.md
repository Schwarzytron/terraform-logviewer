# Terraform Log Viewer

Веб-сервис для анализа и визуализации логов Terraform, преобразующий неструктурированные логи в удобный для анализа формат с расширенными возможностями поиска и фильтрации.

## 🚀 Возможности (wip)

### 📊 Парсинг и анализ (wip)
- **Автоматическое распознавание секций** - выделение terraform plan и apply
- **Эвристический парсинг** - определение временных меток и уровней логирования даже при их отсутствии
- **Извлечение JSON-блоков** - автоматическое обнаружение и парсинг tf_http_req_body и tf_http_res_body
- **Группировка по tf_req_id** - объединение связанных запросов и ответов

### 🔍 Поиск и фильтрация (wip)
- **Полнотекстовый поиск** по всем полям логов
- **Фильтрация по уровням** (ERROR, WARN, INFO, DEBUG)
- **Фильтрация по секциям** (plan, apply, other)
- **Поиск по нескольким полям** с поддержкой сложных условий

### 📈 Визуализация (wip)
- **Цветовая подсветка** уровней логирования
- **Интерактивные JSON-блоки** с возможностью разворачивания/сворачивания
- **Диаграмма Ганта** для визуализации хронологии запросов
- **Статистика** по количеству записей, ошибок и предупреждений

### 🔌 Расширяемость (wip)
- **gRPC-плагины** для подключения пользовательских фильтров и обработчиков
- **REST API** для интеграции с внешними системами
- **Отметка записей** как прочитанных для исключения из анализа

## 🛠 Технологический стек

### Backend
- **Spring Boot 3.5.6** - основной фреймворк
- **Java 21** - язык программирования
- **Elasticsearch** - полнотекстовый поиск
- **gRPC** - система плагинов

### Frontend
- **React 18** - пользовательский интерфейс
- **TypeScript** - типизация
- **Bootstrap** - UI компоненты
- **Vite** - сборка и разработка

### Инфраструктура
- **Docker** - контейнеризация
- **Docker Compose** - оркестрация
- **Gradle** - сборка проекта

## 📦 Быстрый старт

### Предварительные требования
- Docker и Docker Compose
- 4GB+ свободной оперативной памяти

### Запуск проекта

**Клонирование репозитория**
```bash
git clone https://github.com/Schwarzytron/terraform-logviewer
cd terraform-logviewer
docker-compose up -d
```
**Доступ к приложению**

Frontend: http://localhost:3000

Backend API: http://localhost:8080

PostgreSQL: localhost:5432

Elasticsearch: http://localhost:9200

**Ручная сборка и запуск**
Backend:
```bash
cd backend
./gradlew bootRun
```
Frontend:
```bash
cd frontend
npm install
npm run dev
```
## 📁 Структура проекта
```text
terraform-logviewer/
├── backend/                 # Spring Boot приложение
│   ├── src/main/java/
│   │   └── ru/konkurst1/ekb/terraform_logviewer/
│   │       ├── controller/  # REST контроллеры
│   │       ├── service/     # Бизнес-логика
│   │       ├── repository/  # Доступ к данным
│   │       ├── model/       # Сущности
│   │       └── dto/         # Data Transfer Objects
│   └── src/main/resources/
│       └── application.yml # Конфигурация
├── frontend/               # React приложение
│   ├── src/
│   │   ├── components/     # React компоненты
│   │   ├── types/          # TypeScript типы
│   │   └── App.tsx         # Основной компонент
│   └── vite.config.ts      # Конфигурация Vite
└── docker-compose.yml      # Docker композ
```
## 🔧 Конфигурация
Основные настройки (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://db:5432/logviewer
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    ru.konkurst1.ekb.terraform_logviewer: DEBUG
```
### Переменные окружения
SPRING_PROFILES_ACTIVE - активные профили (dev, prod, docker)

DATABASE_URL - URL базы данных

ELASTICSEARCH_URL - URL Elasticsearch

SERVER_PORT - порт backend приложения

## 📋 Использование
1. Загрузка логов
   Перейдите на http://localhost:3000

Нажмите "Выбрать файл" и загрузите Terraform лог (.log, .txt, .json)

Дождитесь завершения парсинга

2. Просмотр и фильтрация
   Используйте фильтры по секциям и уровням для сужения результатов

Разворачивайте JSON-блоки кликом для детального просмотра

Используйте поиск для быстрого нахождения нужных записей

3. Анализ
   Просматривайте статистику в верхней панели

Анализируйте временные линии на диаграмме Ганта

Группируйте записи по tf_req_id для трассировки запросов

## 🔌 API Endpoints
Основные endpoints
POST /api/logs/upload - загрузка файла логов

GET /api/logs/entries - получение записей с пагинацией

GET /api/logs/search - поиск по содержимому

GET /api/logs/stats - статистика

GET /api/logs/files - список загруженных файлов

Пример использования API
```bash
# Загрузка логов
curl -X POST -F "file=@terraform.log" http://localhost:8080/api/logs/upload

# Поиск записей
curl "http://localhost:8080/api/logs/search?query=error&logFileId=123"
```
## 🎯 Критерии оценивания

|Критерий|	Баллы|	Статус|
| ---|---|---|
|Парсинг и маркировка секций	|20|	✅|
|Поиск и фильтрация	|20	|✅|
|Расширяемость и плагины	|20	|🔄|
|Визуализация хронологии	|20	|🔄|
|Презентация и концепция	|20	|🔄|
## 🤝 Разработка
Установка для разработки
Backend разработка
```bash
cd backend
./gradlew build
./gradlew bootRun
```
Frontend разработка
```bash
cd frontend
npm install
npm run dev
```
Тестирование
```bash
# Backend тесты
./gradlew test
# Frontend тесты
npm test
```
## 📊 Архитектура
```text
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   React         │    │   Spring Boot    │    │   PostgreSQL    │
│   Frontend      │◄──►│   Backend        │◄──►│   Database      │
│                 │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              │
                      ┌──────────────────┐
                      │   Elasticsearch  │
                      │   Search Engine  │
                      └──────────────────┘
```
## 📝 Лицензия
Этот проект разработан в рамках учебного задания.

## 👥 Команда разработки
Разработано командой big brains think alike для решения задач анализа логов Terraform в промышленных масштабах.

Примечание: Проект находится в активной разработке. Некоторые (или даже почти все) функции могут быть в процессе реализации.
