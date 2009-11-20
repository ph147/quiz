import java.io.*;
import java.util.*;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class quiz extends MIDlet implements CommandListener {

    // Personal settings
    private int timeBetweenHints = 10;
    private int timeBetweenQuestions = 0;
    private int numberOfHints = 5;

    private Command exitCommand, typeCommand, backCommand;
    private Form form;
    private Timer timer;
    private TestTimerTask task;
    private StringItem curItem;
    private TextBox textBox;
    private Date date;

    private boolean gameFocused, active;

    private int hintCounter, questionNumber, revealedLetters;
    private String question, answer, hint;

    // TODO: read from file
    private int numberOfBytes = 56400;
    private int numberOfQuestions = 1000;

    private String filename = "questions.de";

    public quiz() {
        backCommand = new Command("Enter", Command.BACK, 1);
        typeCommand = new Command("Type", Command.SCREEN, 1);
        exitCommand = new Command("Exit", Command.EXIT, 2);

        form = new Form("Quiz");
        form.addCommand(exitCommand);
        form.addCommand(typeCommand);
        form.setCommandListener(this);
    }

    protected void startApp() {
        startQuiz();
        Display.getDisplay(this).setCurrent(form);
    }

    protected void pauseApp() {}
    protected void destroyApp(boolean bool) {}

    public void commandAction(Command cmd, Displayable disp) {
        if (cmd == typeCommand) {
            openTypeDialogue();
        } else if (cmd == backCommand) {
            gameFocused = true;
            Display.getDisplay(this).setCurrent(form);
            curItem = new StringItem("", "<User> "+textBox.getString()+"\n");
            form.append(curItem);
            if (active) {
                Display.getDisplay(this).setCurrentItem(curItem);
            }
            checkResponse(textBox.getString());
        } else if (cmd == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
        }
    }

    public void startQuiz() {
        gameFocused = true;
        questionNumber = 0;

        getNextQuestion();
        startTimer();
    }

    public void getNextQuestion() {
        readFile();

        hintCounter = 0;
        revealedLetters = 0;
        active = true;

        curItem = new StringItem("Frage Nr. "+ ++questionNumber+": ", question+"\n");
        form.append(curItem);

        date = new Date();

        if (gameFocused) {
            Display.getDisplay(this).setCurrentItem(curItem);
        }
    }

    public void readFile() {
        Random rand = new Random(System.currentTimeMillis());
        int randomInt = rand.nextInt()%numberOfBytes;
        int counter = 0;
        int temp;

        if (randomInt < 0) {
            randomInt += numberOfBytes;
        }
        randomInt++;

        try {
            InputStreamReader reader = new InputStreamReader(
                    getClass().getResourceAsStream(filename));

            skip(reader, randomInt);

            String strLine = readLine(reader);
            temp = strLine.indexOf("|");
            question = strLine.substring(0,temp);
            answer = strLine.substring(temp+1,strLine.length());
            reader.close();
        } catch (Exception e) {
            // File not found
        }
    }

    // Skip number of bytes in 8kb chunks
    public void skip (InputStreamReader reader, long bytes) throws IOException {
        int readChar;
        int chunk = 8024;
        char temp[] = new char[chunk];
        long counter = 0;
        while (counter+chunk < bytes) {
            reader.read(temp, 0, chunk);
            counter += chunk;
        }
        for (int i = 0; i < bytes-counter; ++i) {
            readChar = reader.read();
        }
    }

    public static String replaceAll(String _text, String _searchStr, String _replacementStr) {
        StringBuffer sb = new StringBuffer();
        int searchStringPos = _text.indexOf(_searchStr);
        int startPos = 0;
        int searchStringLength = _searchStr.length();

        while (searchStringPos != -1) {
            sb.append(_text.substring(startPos, searchStringPos)).append(_replacementStr);
            startPos = searchStringPos + searchStringLength;
            searchStringPos = _text.indexOf(_searchStr, startPos);
        }
        sb.append(_text.substring(startPos,_text.length()));
        return sb.toString();
    }

    private String readLine(InputStreamReader reader) throws IOException {
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        }

        while (readChar != -1 && readChar != '\n') {
            readChar = reader.read();
        }
        readChar = reader.read();

        StringBuffer string = new StringBuffer("");
        while (readChar != -1 && readChar != '\n') {
            if (readChar != '\r') {
                string.append((char)readChar);
            }
            readChar = reader.read();
        }
        return string.toString();
    }

    public void getNextHint() {
        hint = "";
        if (revealedLetters == answer.length()) {
            showSolution(false);
        } else {
            for (int i=0; i < answer.length(); i++) {
                if ((i-hintCounter)%numberOfHints == 0) {
                    hint += answer.substring(i,i+1);
                    revealedLetters++;
                } else {
                    hint += "·";
                }
            }
            curItem = new StringItem("Tip "+ ++hintCounter+": ",hint+"\n");
            form.append(curItem);
            if (gameFocused) {
                Display.getDisplay(this).setCurrentItem(curItem);
            }
        }
    }

    public String process(String r) {
        //TODO: Regexp, Umlaute
        r = r.toLowerCase();
        r = replaceAll(r,"ä","ae");
        r = replaceAll(r,"ü","ue");
        r = replaceAll(r,"ö","oe");
        r = replaceAll(r,"é","e");
        r = replaceAll(r,"è","e");
        return r;
    }

    public void checkResponse(String r) {
        if (process(r).indexOf(process(answer))>-1) {
            showSolution(true);
        }
    }
    
    public void showSolution(boolean answered) {
        if (active) {
            Date temp = new Date();
            long elapsed = (temp.getTime()-date.getTime())/1000;

            if (!answered) {
                form.append("Automatisch aufgelöst nach "+elapsed+" Sekunden.\n");
            } else {
                curItem = new StringItem("Richtige Antwort ", "nach "+elapsed+" Sekunden.\n");
                form.append(curItem);
            } 
            curItem = new StringItem("Die Lösung war: ", answer);
            form.append(curItem);
            if (gameFocused) {
                Display.getDisplay(this).setCurrentItem(curItem);
            }
            active = false;
            timer.cancel();
            startTimer();
        }
    }

    public void openTypeDialogue() {
        textBox = new TextBox("Type", "", 256, 0);
        textBox.addCommand(backCommand);
        textBox.setCommandListener(this);
        gameFocused = false;
        Display.getDisplay(this).setCurrent(textBox);
    }

    public void startTimer() {
        timer = new Timer();
        task = new TestTimerTask();

        if (active) {
            timer.schedule(task,timeBetweenHints*1000,timeBetweenHints*1000);
        } else {
            timer.schedule(task,timeBetweenQuestions*1000);
        }
    }

    private class TestTimerTask extends TimerTask {
        public final void run() {
            if (active) {
                getNextHint();
            } else {
                getNextQuestion();
                startTimer();
            }
        }
    }
}
