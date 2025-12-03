#!/usr/bin/env python3
"""
Скрипт для создания администратора в базе данных
Использует ту же версию BCrypt, что и в приложении Spring
"""

import bcrypt
import subprocess
import sys

def generate_bcrypt_hash(password):
    """Генерирует BCrypt хэш пароля, совместимый с Spring Security"""
    salt = bcrypt.gensalt(rounds=10)
    hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed.decode('utf-8')

def run_psql_command(sql_command):
    """Выполняет команду в PostgreSQL через Docker"""
    cmd = [
        'docker', 'exec', 'education_db',
        'psql', '-U', 'postgres', '-d', 'education_platform',
        '-c', sql_command
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout, None
    except subprocess.CalledProcessError as e:
        return None, e.stderr

def main():
    print("Создание/обновление пользователя администратора...")
    
    password = "admin123"
    hash_value = generate_bcrypt_hash(password)
    
    print(f"Сгенерированный хэш: {hash_value}")
    print(f"Длина хэша: {len(hash_value)} символов")
    
    # Проверяем существование администратора
    stdout, stderr = run_psql_command("SELECT id FROM users WHERE username='admin';")
    
    if stderr:
        print(f"Ошибка при проверке пользователя: {stderr}")
        sys.exit(1)
    
    if "0 rows" in stdout:
        print("Создание нового пользователя администратора...")
        sql = f"INSERT INTO users (username, password, email, role) VALUES ('admin', '{hash_value}', 'admin@example.com', 'ADMIN');"
    else:
        print("Обновление пароля существующего пользователя администратора...")
        sql = f"UPDATE users SET password = '{hash_value}' WHERE username='admin';"
    
    stdout, stderr = run_psql_command(sql)
    
    if stderr:
        print(f"Ошибка при обновлении БД: {stderr}")
        sys.exit(1)
    
    print("Успешно!")
    
    # Проверяем результат
    stdout, stderr = run_psql_command("SELECT username, email, role, length(password) as password_length FROM users WHERE username='admin';")
    if stdout:
        print("\nУтвержденные данные:")
        print(stdout)
    
    print("\nДля входа используйте:")
    print("Логин: admin")
    print("Пароль: admin123")

if __name__ == "__main__":
    main()
