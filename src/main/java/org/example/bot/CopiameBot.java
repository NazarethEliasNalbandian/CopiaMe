package org.example.bot;

import org.example.dominio.Analizador.AnalisisDeCopia;
import org.example.dominio.Analizador.ParDocumento;
import org.example.dominio.Documento.Lote;
import org.example.dominio.Reporte.ResultadoLote;
import org.example.unzip.UnzipUtility;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.TelegramBotsApi;

public class CopiameBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message.hasDocument()) {
            Document document = message.getDocument();
            if (document.getMimeType().equals("application/zip")) {
                try {
                    // Obtiene el archivo
                    GetFile getFile = new GetFile();
                    getFile.setFileId(message.getDocument().getFileId());
                    org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
                    java.io.File downloadedFile = downloadFile(file);

                    // Descomprime los archivos en un directorio
                    String destDirectory = "src/test/resources/documentos/" + message.getDocument().getFileId();
                    UnzipUtility.unzip(downloadedFile, destDirectory);

                    // Procesa al lote
                    Lote lote = new Lote(destDirectory);
                    lote.validar(destDirectory);
                    lote.cargar();
                    float umbral = 0.5f;
                    AnalisisDeCopia analisis = new AnalisisDeCopia(umbral, lote);
                    ResultadoLote resultado = analisis.procesar();

                    // Genera la salida y manda el mensaje
                    StringBuilder se_copiaron = new StringBuilder();
                    for (ParDocumento par : resultado.getPosiblesCopias()) {
                        se_copiaron.append(par.getDocumento1().getAutor()).append(" ").append(par.getDocumento2().getAutor()).append("\n");
                    }

                    // Envia el mensaje al usuario
                    SendMessage responseMsg = new SendMessage();
                    responseMsg.setChatId(message.getChatId());
                    if(se_copiaron.toString().isBlank()) {
                        responseMsg.setText("No se copió nadie");
                    } else {
                        responseMsg.setText("Se copiaron: \n" + se_copiaron);
                    }
                    execute(responseMsg);
                } catch (Exception e ) {
                    e.printStackTrace();
                }
            }
        }else {
            // Esta función se invocará cuando nuestro bot reciba un mensaje
            // Se obtiene el mensaje escrito por el usuario
            SendMessage writtenMessage = getWrittenMessage(update);

            try {
                // Se envía el mensaje
                execute(writtenMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private static SendMessage getWrittenMessage(Update update) {
        final String messageTextReceived = update.getMessage().getText().toLowerCase();

        // Se obtiene el id de chat del usuario
        Long chatId = update.getMessage().getChatId();

        // Se crea un objeto mensaje
        SendMessage writtenMessage = new SendMessage();
        writtenMessage.setChatId(chatId.toString());

        if (messageTextReceived.contains("hola")) {
            writtenMessage.setText("Hola, viste q Giorgio es homosexual");
        } else if (messageTextReceived.contains("todo")){
            writtenMessage.setText("No, todo mal (por culpa de Giorgio)");
        } else {
            writtenMessage.setText("No te enseñaron a saludar?");
        }
        return writtenMessage;
    }

    @Override
    public String getBotUsername() {
        return System.getenv("NOMBRE_BOT");
    }
    @Override public String getBotToken() {
        return System.getenv("TOKEN_BOT");
    }
    
    public static void main(String[] args) throws TelegramApiException {
        // Se crea un nuevo Bot API
        final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

        try {
            // Se registra el bot
            telegramBotsApi.registerBot(new CopiameBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
