-- Инициализация базы данных для платформы онлайн обучения

-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER'
);

-- Создание таблицы курсов (если не существует)
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    author_id BIGINT
);

-- Примечание: Тестовых пользователей нужно создавать через API регистрации,
-- так как BCrypt генерирует уникальные хеши каждый раз.
-- Пример:
-- curl -X POST http://localhost:8080/api/auth/register \
--   -H "Content-Type: application/json" \
--   -d '{"username":"admin","password":"admin123","email":"admin@example.com"}'

-- Вставка примеров данных
INSERT INTO courses (title, description, category, author_id) VALUES
('Java для начинающих', 'Базовый курс по программированию на Java', 'Программирование', 1),
('Spring Boot Мастер-класс', 'Продвинутый курс по Spring Boot', 'Программирование', 1),
('Docker и Kubernetes', 'Контейнеризация и оркестрация', 'DevOps', 2),
('PostgreSQL для разработчиков', 'Работа с реляционными базами данных', 'Базы данных', 2),
('Frontend разработка', 'HTML, CSS, JavaScript основы', 'Web-разработка', 3)
ON CONFLICT DO NOTHING;
