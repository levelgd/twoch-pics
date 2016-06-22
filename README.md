# Описание
Сервер на Node.js периодически собирает ссылки на картинки через API сайта [2ch.hk](https://2ch.hk).
Клиент обращается к серверу и тот передает ссылки клиенту, после чего клиент загружает картинки по ссылкам и демонстрирует их в виде галереи.

Демо: https://sosach.herokuapp.com/

# Инструкции по установке
### Сервер:
Сервер готов для работы в облачном сервисе [Heroku](https://www.heroku.com/)

Для запуска на локальном хосте требуется Node.js 4.x версии с установленными модулями
- **express** версии 4.13.x
- **ejs** версии 2.3.x
- **request** версии 2.67.x
- **queue** версии 3.1.x

Работоспособность сервера с более новыми версиями этих модулей и другими версиями Node.js не проверялась.

### Клиент:
Требуется Android SDK, JDK и IDE, поддерживающая создание Android-приложений (Автор этих строк пользуется IntelliJ IDEA Community Edition).
Хардкод, который нужно исправить на адрес своего сервера:
В файле `MainActivity.java` строчки 230, 403, 436 и 500;
В файле `PicActivity.java` строка 127.
