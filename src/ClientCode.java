import java.io.*;
import java.net.Socket;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class Main {
    public static int PORT = 8000;
    public static String IP = "127.0.0.1";
    public static int buffer = 255;

    //имя получаем из командной строки
    public static void main(String[] args) throws IOException {
        String name; // имя пользователя
        String path = "default";
        if (args.length == 0){
            name = "anon"; // если аргументов командной строки нет, имя пользователя - "anon", путь до файла по-умолчанию
            System.out.println("name anon");
            System.out.println("path default");
        } else if (args.length == 1){
            name = args[0];
            path = "default";
            System.out.println("name "+ name);
            System.out.println("path default");
        }else{
            name = args[0];
            path = args[1];
            System.out.println("name " + name);
            System.out.println("path " + path);
        }
        if (!path.equals("default")){
            try{
                File file = new File(path);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                PORT = Integer.parseInt(bufferedReader.readLine());
                IP = bufferedReader.readLine();
                buffer = Integer.parseInt(bufferedReader.readLine());
                System.out.println("Port = " + PORT);
                System.out.println("IP = " + IP);
                System.out.println("buffer = " + buffer);
            }catch (FileNotFoundException e){
                System.out.println("Cannot find config file. Program will use default values");
            }
        }
        new ClientListener(IP, PORT, name); //запускаем клиент
    }

}

class ClientListener {
    private Socket socket;
    private BufferedReader in; // буферезированный поток на ввод в консоль
    private BufferedWriter out; // буферизированный поток на получение из сокета
    private BufferedReader userInp; // буферизированный поток на запись в сокет
    private String name; // имя пользователя

    public ClientListener(String ip, int port, String userName) throws IOException {
        try {
            this.socket = new Socket(ip, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            name = userName;
            userInp = new BufferedReader(new InputStreamReader(System.in), Main.buffer);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()), Main.buffer);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()), Main.buffer);
            new ReadMsg().start();
            new WriteMsg().start();
        } catch (IOException e) {
            in.close();
            out.close();
            socket.close();
        }
    }

    public class WriteMsg extends Thread {
        @Override
        public void run() {
            while (true) {
                String userWord;
                try {
                    userWord = userInp.readLine();

                    char nameLength = (char) name.length();

                    if (userWord.length() >= Main.buffer) {
                        userWord = userWord.substring(0, Main.buffer - 4);
                        userWord = userWord + "...";
                    }
                    char userWordLength = (char) userWord.length();
                    String output = nameLength + name + userWordLength + userWord;
                    out.write(output + "\n");

                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * класс, принимающий сообщения от сервера
     * in - входной поток
     * str - полученная строка из входного потока
     */
    private class ReadMsg extends Thread {
        /**
         * запуск класса чтения сообщения в отдельном потоке
         */
        @Override
        public void run() {
            String str;
            try {
                while (true) {
                    str = in.readLine();
                    if (str != null && !str.isEmpty()){ //строка не сразу успевает прийти, но так как цикл итерирует часто, в какой-то момент эта проверка сработает
                        consoleWriter(str);
                    }
                }
            } catch (IOException e) {
                System.out.println("Server disconnected. Disconnect clients");
            }
        }

        /**
         *
         * @param str - принимаемая от сервера сырая строка с сообщением
         *            Метод принимает сырую строку, парсит её, работая по факту имплементацией протокола для отображения
         *            полученной сырой строки.
         */
        private void consoleWriter(String str){
            int dateLength = str.charAt(0); // получаем длину даты
            String dateGMTStr = str.substring(1, dateLength+1);// получаем строку даты
            int nameLength = str.charAt(dateLength+1); // получаем длину имени
            String name = str.substring(2+dateLength, 2+dateLength+nameLength); // получаем строку имени
            int msgLength = str.charAt(2+dateLength+nameLength); // получаем длину сообщения
            String message = str.substring(3+dateLength+nameLength, 3+dateLength + nameLength + msgLength); // получаем сообщение
            //приведение даты в формат местного времени из Гринвича
            Calendar calendar = new GregorianCalendar();
            TimeZone mTimeZone = calendar.getTimeZone();
            int gmtRawOffset = mTimeZone.getRawOffset(); // offset в миллисекундах
            int gmtOffset = (int)TimeUnit.HOURS.convert(gmtRawOffset, TimeUnit.MILLISECONDS); // offset в часах
            String dateUTCStr = timeConverter(dateGMTStr, gmtOffset); //вызов метода конвертера времени и получения местного времени
            System.out.println("<" + dateUTCStr + "> [" + name + "] " + message); // формирование строки

        }

        /**
         * @param GMTTime - время в Гринвиче
         * @param offset - часовой пояс относительно Гринвича( + или -)
         * @return время в конкретном часовом поясе
         */
        private String timeConverter(String GMTTime, int offset){
            String hoursString = GMTTime.substring(0,2);
            int hours = Integer.parseInt(hoursString);
            if(hours + offset > 23 && offset > 0){
                hours = hours - 24;
                int offsetTime = hours + offset;
                return offsetTime + GMTTime.substring(2);
            }else if (hours + offset < 0 && offset < 0){
                hours = hours + 24;
                int offsetTime = hours + offset;
                return offsetTime + GMTTime.substring(2);
            }else{
                int offsetTime = hours + offset;
                return offsetTime + GMTTime.substring(2);
            }
        }
    }
}




