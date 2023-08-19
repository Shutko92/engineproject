# SearchEngine
Представляет собой поисковый движок (включая веб-интерфейс), содержащий необходимые средства 
для полноценного функционирования:
* возможность предварительной индексации сайтов. При этом обходятся сайты, чьи параметры были заданы в конфигурационном файле.
Программа собирает данные страниц и анализирует леммы на них, связывая все вместе.
* возможность для просмотра сводки статистики - какие сайты используются, их состояние, количество страниц и лемм.
* возможность для поиска по ключевым словам и вывода страниц по релевантности.
***
## Стек технологий
* Java 17
* Maven
* Spring Boot
* MySQL 8
* Hibernate
* Lombok
* Jsoup
* Slf4j
***
## Инструкция по подключению
* Перед запуском установите на свой компьютер MySQL-сервер, если он ещё не установлен, 
и создайте в нём пустую базу данных search_engine. Рекомендуется использовать кодировку utf8mb4. Также вам нужно указать токен через файл settings.xml.
#### В Windows он располагается в директории
#### C:/Users/<Имя вашего пользователя>/.m2
#### В Linux — в директории
#### /home/<Имя вашего пользователя>/.m2
#### В macOs — по адресу
#### /Users/<Имя вашего пользователя>/.m2
Если файла *settings.xml* нет, создайте и вставьте в него:  
</b></details>
<details>
<summary> settings.xml </summary><br><b>

```xml  
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>skillbox-gitlab</id>
            <configuration>
                <httpHeaders>
                    <property>
                        <name>Private-Token</name>
                        <value>wtb5axJDFX9Vm_W1Lexg</value>
                    </property>
                </httpHeaders>
            </configuration>
        </server>
    </servers>
</settings>
```
</b></details>

Если файл уже есть, то добавьте только блок *server* в *servers*

* В конфигурационном файле application.yaml пропишите нужные данные доступа к MySQL-серверу. Кроме того, в том же файле пропишите ссылки и названия нужных сайтов.
* Если все правильно, то после запуска приложения в браузере, по ссылке http://localhost:8080/, будет отображаться интерфейс.
* ***
## Инструкция по использованию
* Во вкладке Management запустите индексацию сайтов кнопкой start indexing. Она может занять долгое время.
* Во вкладке search либо выберите конкретный сайт, либо поиск будет проводиться по всем имеющимся сайтам.
* Сформулируйте запрос в нижней строке и запустите поиск.
