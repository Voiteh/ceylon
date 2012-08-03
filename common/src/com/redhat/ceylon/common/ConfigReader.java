package com.redhat.ceylon.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.InvalidPropertiesFormatException;

public class ConfigReader {
    private ConfigReaderListener listener;
    private InputStream in;
    private LineNumberReader counterdr;
    private MemoPushbackReader reader;
    private String section;
    
    private enum Token { section, option, assign, comment, eol, error, eof };
    
    public ConfigReader(InputStream in, ConfigReaderListener listener) {
        this.in = in;
        this.listener = listener;
    }
    
    public void process() throws IOException {
        section = null;
        try {
            counterdr = new LineNumberReader(new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8"))));
            reader = new MemoPushbackReader(counterdr);

            Token tok;
            skipWhitespace(true);
            flushWhitespace();
            while ((tok = peekToken()) != Token.eof) {
                switch (tok) {
                case section:
                    handleSection();
                    break;
                case option:
                    if (section != null) {
                        handleOption();
                    } else {
                        throw new InvalidPropertiesFormatException("Option without section in configuration file at line " + (counterdr.getLineNumber() + 1));
                    }
                    break;
                case comment:
                    skipToNextLine();
                    listener.onComment(reader.getAndClearMemo());
                    break;
                case eol:
                    skipToNextLine();
                    listener.onWhitespace(reader.getAndClearMemo());
                    break;
                default:
                    throw new InvalidPropertiesFormatException("Unexpected token in configuration file at line " + (counterdr.getLineNumber() + 1));
                }
                skipWhitespace(true);
                flushWhitespace();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void handleSection() throws IOException {
        expect('[');
        section = readName(true).toLowerCase();
        if (!section.matches("[\\p{L}\\p{Nd}]+(\\.[\\p{L}\\p{Nd}]+)*")) {
            throw new InvalidPropertiesFormatException("Invalid section name in configuration file at line " + (counterdr.getLineNumber() + 1));
        }
        skipWhitespace(false);
        if (peek() == '\"') {
            String subSection = readString();
            expect('"');
            section += "." + subSection;
            skipWhitespace(false);
        }
        expect(']');
        listener.onSection(section, reader.getAndClearMemo());
    }
    
    private void handleOption() throws IOException {
        String option = readName(false);
        String optName = section + "." + option;
        String value;
        skipWhitespace(false);
        Token tok = peekToken();
        if (tok == Token.assign) {
            expect('=');
            value = readValue();
        } else if (tok == Token.error) {
            throw new InvalidPropertiesFormatException("Unexpected token in configuration file at line " + (counterdr.getLineNumber() + 1));
        } else {
            value = "true";
        }
        listener.onOption(optName, value, reader.getAndClearMemo());
    }

    private String readName(boolean forSection) throws IOException {
        StringBuilder str = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            if ((!forSection && isOptionNameChar(c)) || (forSection && isSectionNameChar(c))) {
                str.append((char)c);
            } else {
                reader.unread(c);
                break;
            }
        }
        return str.toString();
    }

    private String readString() throws IOException {
        StringBuilder str = new StringBuilder();
        gobble('\"');
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '"') {
                reader.unread(c);
                break;
            } else if (c == '\\') {
                int c2 = reader.read();
                if (c2 == '\\') {
                    // Do nothing
                } else if (c2 == '\"') {
                    c = c2;
                } else {
                    throw new InvalidPropertiesFormatException("Illegal escape character in configuration file at line " + (counterdr.getLineNumber() + 1));
                }
            }
            str.append((char)c);
        }
        return str.toString();
    }

    private String readValue() throws IOException {
        StringBuilder str = new StringBuilder();
        skipWhitespace(false);
        boolean hasQuote = gobble('\"');
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '"') {
                reader.unread(c);
                break;
            } else if (isNewLineChar(c)) {
                reader.unread(c);
                break;
            } else if (isCommentChar(c) && !hasQuote) {
                break;
            } else if (c == '\\') {
                int c2 = reader.read();
                if (c2 == '\\') {
                    // Do nothing
                } else if (c2 == '\"') {
                    c = c2;
                } else if (c2 == 't') {
                    c = '\t';
                } else if (c2 == 'n') {
                    c = '\n';
                } else if (isNewLineChar(c2)) {
                    skipNewLine(c2);
                    c = '\n';
                } else {
                    throw new InvalidPropertiesFormatException("Illegal escape character in configuration file at line " + (counterdr.getLineNumber() + 1));
                }
            }
            str.append((char)c);
        }
        if (hasQuote) {
            expect('\"');
            return str.toString();
        } else {
            return str.toString().trim();
        }
    }

    private void expect(int expected) throws IOException {
        int c;
        if ((c = reader.read()) != expected) {
            throw new InvalidPropertiesFormatException("Unexpected token in configuration file at line " + (counterdr.getLineNumber() + 1) + ", expected '" + Character.valueOf((char)expected) + "' but got '" + Character.valueOf((char)c) + "'");
        }
    }
    
    private void skipWhitespace(boolean multiline) throws IOException {
        int c;
        while ((c = reader.read()) != -1) {
            if (!Character.isWhitespace(c) || (!multiline && isNewLineChar(c))) {
                reader.unread(c);
                break;
            }
        }
    }
    
    private void skipToNextLine() throws IOException {
        int c;
        while ((c = reader.read()) != -1) {
            if (isNewLineChar(c)) {
                skipNewLine(c);
                break;
            }
        }
    }

    private Token peekToken() throws IOException {
        int c = peek();
        if (isCommentChar(c)) {
            return Token.comment;
        } else if (c == '[') {
            return Token.section;
        } else if (c == '=') {
            return Token.assign;
        } else if (isNewLineChar(c)) {
            return Token.eol;
        } else if (isOptionNameChar(c)) {
            return Token.option;
        } else if (c == -1) {
            return Token.eof;
        } else {
            return Token.error;
        }
    }
    
    private int peek() throws IOException {
        int c = reader.read();
        reader.unread(c);
        return c;
    }
    
    private boolean gobble(int chr) throws IOException {
        int c = reader.read();
        if (c != chr) {
            reader.unread(c);
            return false;
        }
        return true;
    }
    
    private void skipNewLine(int c) throws IOException {
        if (c == '\r') {
            c = reader.read();
            if (c != '\n') {
                reader.unread(c);
            }
        }
    }
    
    private void flushWhitespace() {
        String ws = reader.getAndClearMemo();
        if (!ws.isEmpty()) {
            listener.onWhitespace(ws);
        }
    }
    
    private boolean isOptionNameChar(int c) {
        return Character.isLetterOrDigit(c) || c == '-';
    }
    
    private boolean isSectionNameChar(int c) {
        return isOptionNameChar(c) || c == '.';
    }
    
    private boolean isCommentChar(int c) {
        return c == ';' || c == '#';
    }
    
    private boolean isNewLineChar(int c) {
        return c == '\n' || c == '\r';
    }
}

class MemoPushbackReader extends PushbackReader {
    private StringBuilder memo;
    
    public MemoPushbackReader(Reader in) {
        super(in);
        memo = new StringBuilder(1024);
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        memo.append((char)c);
        return c;
    }

    @Override
    public void unread(int c) throws IOException {
        super.unread(c);
        memo.setLength(memo.length() - 1);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        memo.setLength(0);
    }

    public String getAndClearMemo() {
        String result = memo.toString();
        memo.setLength(0);
        return result;
    }
    
    // All the following methods we don't really need so they aren't implemented
    // to prevent anybody from accidentally using them they throw an error
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unread(char[] cbuf, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unread(char[] cbuf) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        throw new UnsupportedOperationException();
    }
}