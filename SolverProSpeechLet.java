package com.def.max.SpeechLetHandler;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClient;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesRequest;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesResult;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeClient;
import com.amazonaws.services.transcribe.model.*;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import com.def.max.Models.TranscriptionModel;
import com.def.max.Models.TranscriptionResultsTranscripts;
import com.def.max.Utils.GoogleWebSearch;
import com.def.max.Utils.SearchQuery;
import com.def.max.Utils.SearchResult;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolverProSpeechLet implements SpeechletV2
{
    private Logger logger = LogManager.getLogger(SolverProSpeechLet.class);

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> speechletRequestEnvelope)
    {
        logger.debug("Session started at : " + speechletRequestEnvelope.getRequest().getTimestamp());
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> speechletRequestEnvelope)
    {
        Session session = speechletRequestEnvelope.getSession();

        String welcomeMessage = "Hi, welcome to solver pro. " +
                "My work is to analyse the image files, audio files and voice texts. " +
                "And then i can perform some tasks on your files and voice texts. " +
                "And after finishing the task, i will say the results of task. " +
                "Okay, i listed below what i will can do with your files and voice texts. " +
                "I listed below the task names. " +
                "Your work is to choose one task name from list and say it to me with the keyword task name. " +
                "Example, task name image moderation. " +
                "Okay, now i say the task names one by one, first one is image moderation, " +
                "second one is image object and scene detection, " +
                "third one is image text detection, " +
                "fourth one is image face comparison, " +
                "fifth one is audio extract text, " +
                "sixth one is text translate, " +
                "seventh one is text key phrase detection, " +
                "and finally eight one is text definition. " +
                "Okay, this are tasks i will do on your files and voice texts. " +
                "Now, choose one task name from above list. " +
                "And say the task name with the keyword task name. " ;

        String cardTitle = "Welcome Message Card";

        String welcomeRePromptMessage = "Now, choose one task name from above list. " +
                "And say the task name with the keyword task name. " ;

        logger.debug(cardTitle + " : " + welcomeMessage);

        session.setAttribute("repeat_message",welcomeMessage);
        session.setAttribute("repeat_re_prompt_message",welcomeRePromptMessage);

        return getMessageWithSimpleCardResponse(welcomeMessage,cardTitle,welcomeRePromptMessage,true);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> speechletRequestEnvelope)
    {
        Intent intent = speechletRequestEnvelope.getRequest().getIntent();

        Session session = speechletRequestEnvelope.getSession();

        if (intent != null)
        {
            String intentName = intent.getName();

            if (intentName != null)
            {
                switch (intentName) {
                    case "GetTaskNameIntent": {
                        String taskName = intent.getSlot("task_name").getValue();

                        if (taskName.isEmpty()) {
                            return getTaskNameRecognizeEmptyResponse(session);
                        } else {
                            switch (taskName) {
                                case "image moderation":
                                case "image object and scene detection":
                                case "image face comparison":
                                case "image text detection":
                                    return getFileTypeTaskResponse(taskName, session);
                                case "audio extract text": {
                                    String audioExtractTextMessage = "Okay, you choose audio extract text task, this task is useful to extract text from the your audio file. " +
                                            "Okay, keep it mind please use small size files less than 10 mb. " +
                                            "Okay, you choose file type task, that means we want some storage place to store and retrieve the image files called s3 storage. " +
                                            "I hope you already know the bucket name,file name and file format. " +
                                            "Or, if you don't know. " +
                                            "Don't worry simply say ' storage helper ' to get the bucket name,file name and file format. " +
                                            "And then another important thing i support only jpg and png file formats. " +
                                            "So, please choose jpg and png images only from your storage. " +
                                            "Now, you know how to say the bucket name,file name and file format to me, simply say the bucket name with the keyword bucket name. " +
                                            "Example, bucket name my bucket. " +
                                            "Otherwise if you don't how to say the bucket name,file name and file format to me. " +
                                            "Don't worry simply say ' file helper '. " +
                                            "Okay, now say the bucket name with keyword bucket name. ";

                                    String cardTitle = "Audio Extract Text Message";

                                    String audioExtractTextRePromptMessage = "Okay, now say the bucket name with keyword bucket name. ";

                                    session.setAttribute("task_name",taskName);

                                    return getMessageWithSimpleCardResponse(audioExtractTextMessage, cardTitle, audioExtractTextRePromptMessage, true);
                                }
                                case "text translate": {
                                    String textTranslateMessage = "Okay, you choose text translate task, this text translate task is useful to convert english paragraph text into 20 other languages text. " +
                                            "I can understand only english. " +
                                            "So, please say the text in english. " +
                                            "Ok, now say the paragraph text to convert other language through your voice with the keyword paragraph text. ";

                                    String cardTitle = "Text Translate Task Message";

                                    String textTranslateRePromptMessage = "Ok, now say the text or paragraph to convert other language through your voice with the keyword paragraph text. ";

                                    session.setAttribute("task_name", taskName);

                                    return getMessageWithSimpleCardResponse(textTranslateMessage, cardTitle, textTranslateRePromptMessage, true);
                                }
                                case "text key phrase detection": {
                                    String textKeyPhraseDetectionMessage = "Okay, you choose text key phrase detection task, this text key phrase detection task is useful to get important key phrase from the paragraph. " +
                                            "I can understand only english. " +
                                            "So, please say the text paragraph in english. " +
                                            "Ok, now say the paragraph text to get key phrases through your voice with the keyword paragraph text. ";

                                    String cardTitle = "Text Key Phrase Detection Task Message";

                                    String textKeyPhraseDetectionRePromptMessage = "Ok, now say the paragraph text to get key phrases through your voice with the keyword paragraph text. ";

                                    session.setAttribute("task_name", taskName);

                                    return getMessageWithSimpleCardResponse(textKeyPhraseDetectionMessage, cardTitle, textKeyPhraseDetectionRePromptMessage, true);
                                }
                                case "text definition": {
                                    String textDefinitionMessage = "Okay, you choose text definition task, this text definition task is useful to get definition for any word. " +
                                            "Okay, now say any word with the keyword definition word. ";

                                    String cardTitle = "Text Definition Message";

                                    String tetxDefinitionRePromptMessage = "Okay, now say any word with the keyword definition word. ";

                                    session.setAttribute("task_name", taskName);

                                    return getMessageWithSimpleCardResponse(textDefinitionMessage, cardTitle, tetxDefinitionRePromptMessage, true);
                                }
                                default:
                                    return getTaskNameErrorResponse(taskName, session);
                            }
                        }
                    }
                    case "StorageHelperIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        if (taskName == null || taskName.equals("null") || taskName.equals("") || taskName.startsWith("image") || taskName.startsWith("audio")) {
                            return getStorageHelperIntentResponse(session);
                        } else if (taskName.startsWith("text")) {
                            String storageTextMessage = "Storage Helper only for image and audio files. " +
                                    "You choose " + taskName + " task. " +
                                    "So we don't have storage place. " +
                                    "Pass the text through your voice. " +
                                    "Okay, now say the text to perform the " + taskName + " task with the keyword " + taskName + ". " +
                                    "Or if you want to perform another task, please say the task name with the keyword task name. ";

                            String cardTitle = "Storage Text Message";

                            String storageTextRePromptMessage = "Okay, now say the text to perform the " + taskName + " task with the keyword " + taskName + ". " +
                                    "Or if you want to perform another task, please say the task name with the keyword task name. ";

                            logger.debug(cardTitle + " : " + storageTextMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("repeat_message", storageTextMessage);
                            session.setAttribute("repeat_re_prompt_message", storageTextRePromptMessage);

                            return getMessageWithSimpleCardResponse(storageTextMessage, cardTitle, storageTextRePromptMessage, true);
                        } else {
                            return getStorageHelperIntentResponse(session);
                        }
                    }
                    case "FileHelperIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        if (taskName == null || taskName.equals("null") || taskName.equals("") || taskName.startsWith("image") || taskName.startsWith("audio")) {
                            return getFileHelperIntentResponse(session);
                        } else if (taskName.startsWith("text")) {
                            String fileTextMessage = "File Helper only for image and audio files. " +
                                    "You choose " + taskName + " task. " +
                                    "So we don't have storage place. " +
                                    "Pass the text through your voice. " +
                                    "Okay, now say the text to perform the " + taskName + " task with the keyword " + taskName + ". " +
                                    "Or if you want to perform another task, please say the task name with the keyword task name. ";

                            String cardTitle = "File Text Message";

                            String fileTextRePromptMessage = "Okay, now say the text to perform the " + taskName + " task with the keyword " + taskName + ". " +
                                    "Or if you want to perform another task, please say the task name with the keyword task name. ";

                            logger.debug(cardTitle + " : " + fileTextMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("repeat_message", fileTextMessage);
                            session.setAttribute("repeat_re_prompt_message", fileTextRePromptMessage);

                            return getMessageWithSimpleCardResponse(fileTextMessage, cardTitle, fileTextRePromptMessage, true);
                        } else {
                            return getFileHelperIntentResponse(session);
                        }
                    }
                    case "GetBucketNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = intent.getSlot("bucket_name").getValue().toLowerCase();

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameRecognizeEmptyResponse(taskName, session);
                        } else {
                            String getBucketNameMessage = "Confirm " + bucketName + " is your bucket name. " +
                                    "If yes, say yes this is my bucket name. " +
                                    "If no, say no this is not my bucket name. ";

                            String cardTitle = "Confirm Bucket Name Message";

                            logger.debug(cardTitle + " : " + getBucketNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("repeat_message", getBucketNameMessage);
                            session.setAttribute("repeat_re_prompt_message", getBucketNameMessage);

                            return getMessageWithSimpleCardResponse(getBucketNameMessage, cardTitle, getBucketNameMessage, true);
                        }
                    }
                    case "ThisIsMyBucketNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else {
                            String thisIsMyBucketNameMessage = "Okay now say the file name of your file with the keyword file name. " +
                                    "Example, file name orange. ";

                            String cardTitle = "This Is My Bucket Name Message";

                            logger.debug(cardTitle + " : " + thisIsMyBucketNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("repeat_message", thisIsMyBucketNameMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsMyBucketNameMessage);

                            return getMessageWithSimpleCardResponse(thisIsMyBucketNameMessage, cardTitle, thisIsMyBucketNameMessage, true);
                        }
                    }
                    case "ThisIsNotMyBucketNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else {
                            String thisIsNotMyBucketNameMessage = "Okay don't worry please say the bucket name again with the keyword bucket name. " +
                                    "Example, bucket name orange. ";

                            String cardTitle = "This Is Not My Bucket Name Message";

                            logger.debug(cardTitle + " : " + thisIsNotMyBucketNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("repeat_message", thisIsNotMyBucketNameMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsNotMyBucketNameMessage);

                            return getMessageWithSimpleCardResponse(thisIsNotMyBucketNameMessage, cardTitle, thisIsNotMyBucketNameMessage, true);
                        }
                    }
                    case "GetFileNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = intent.getSlot("file_name").getValue().toLowerCase();

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameRecognizeEmptyResponse(taskName, bucketName, session);
                        } else {
                            String getFileNameMessage = "Confirm " + fileName + " is your file name. " +
                                    "If yes, say yes this is my file name. " +
                                    "If no, say no this is not my file name. ";

                            String cardTitle = "Confirm File Name Message";

                            logger.debug(cardTitle + " : " + getFileNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("repeat_message", getFileNameMessage);
                            session.setAttribute("repeat_re_prompt_message", getFileNameMessage);

                            return getMessageWithSimpleCardResponse(getFileNameMessage, cardTitle, getFileNameMessage, true);
                        }
                    }
                    case "ThisIsMyFileNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else {
                            String thisIsMyFileNameMessage = "Okay, now say your file format with the keyword file format. " +
                                    "Example, file format jpg. ";

                            String cardTitle = "This Is My File Name Message";

                            logger.debug(cardTitle + " : " + thisIsMyFileNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("repeat_message", thisIsMyFileNameMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsMyFileNameMessage);

                            return getMessageWithSimpleCardResponse(thisIsMyFileNameMessage, cardTitle, thisIsMyFileNameMessage, true);
                        }
                    }
                    case "ThisIsNotMyFileNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else {
                            String thisIsNotMyFileNameMessage = "Okay don't worry please say the file name again with the keyword file name. " +
                                    "Example, file name apple. ";

                            String cardTitle = "This Is Not My File Name Message";

                            logger.debug(cardTitle + " : " + thisIsNotMyFileNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("repeat_message", thisIsNotMyFileNameMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsNotMyFileNameMessage);

                            return getMessageWithSimpleCardResponse(thisIsNotMyFileNameMessage, cardTitle, thisIsNotMyFileNameMessage, true);
                        }
                    }
                    case "GetFileFormatIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = intent.getSlot("file_format").getValue().toLowerCase();

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatRecognizeEmptyResponse(taskName, bucketName, fileName, session);
                        } else {
                            String getFileFormatMessage = "Confirm " + fileFormat + " is your file format. " +
                                    "If yes, say yes this is my file format. " +
                                    "If no, say no this is not my file format. ";

                            String cardTitle = "Confirm File Format Message";

                            logger.debug(cardTitle + " : " + getFileFormatMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("repeat_message", getFileFormatMessage);
                            session.setAttribute("repeat_re_prompt_message", getFileFormatMessage);

                            return getMessageWithSimpleCardResponse(getFileFormatMessage, cardTitle, getFileFormatMessage, true);
                        }
                    }
                    case "ThisIsMyFileFormatIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else {
                            String hostName = "https://s3.amazonaws.com/";

                            String fileUrl = hostName + bucketName + "/" + fileName + "." + fileFormat;

                            String thisIsMyFileFormatMessage = "Okay, now confirm this " + fileUrl + " is your file url. " +
                                    "If yes, say yes this is my file url. " +
                                    "Or if no, say no this is not my file url. ";

                            String cardTitle = "This Is My File Format Message";

                            logger.debug(cardTitle + " : " + thisIsMyFileFormatMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("repeat_message", thisIsMyFileFormatMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsMyFileFormatMessage);

                            return getMessageWithSimpleCardResponse(thisIsMyFileFormatMessage, cardTitle, thisIsMyFileFormatMessage, true);
                        }
                    }
                    case "ThisIsNotMyFileFormatIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else {
                            String thisIsNotMyFileFormatMessage = "Okay don't worry please say the file format again with the keyword file format. Example, file format jpg. ";

                            String cardTitle = "This Is Not My File Format Message";

                            logger.debug(cardTitle + " : " + thisIsNotMyFileFormatMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("repeat_message", thisIsNotMyFileFormatMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsNotMyFileFormatMessage);

                            return getMessageWithSimpleCardResponse(thisIsNotMyFileFormatMessage, cardTitle, thisIsNotMyFileFormatMessage, true);
                        }
                    }
                    case "ThisIsMyFileUrlIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else {
                            switch (taskName) {
                                case "image moderation": {
                                    String image = fileName + "." + fileFormat;

                                    AmazonRekognition rekognitionClient = getAmazonRekognitionClient();

                                    try {
                                        DetectModerationLabelsRequest request = new DetectModerationLabelsRequest()
                                                .withImage(new Image().withS3Object(new S3Object().withName(image).withBucket(bucketName)));

                                        StringBuilder stringBuilder = new StringBuilder();

                                        DetectModerationLabelsResult result = rekognitionClient.detectModerationLabels(request);

                                        List<ModerationLabel> labels = result.getModerationLabels();

                                        stringBuilder.append("Okay, image moderation task is useful to detects explicit or suggestive adult content and unsafe in your image, and provides confidence scores. I completed the analysis on your image ").append(image).append(". ");

                                        if (labels.size() == 0) {
                                            stringBuilder.append("I could not find any labels in your image. " +
                                                    "So the image does not contains any explicit or suggestive adult content and the image is safe. " +
                                                    "Okay, if you like to perform another task simply say the task name with keyword task name. ");
                                        } else {
                                            stringBuilder.append("Now, i give the name of the labels and also their confidence level. ");

                                            for (int i = 0; i < labels.size(); i++) {
                                                if (i == labels.size() - 1) {
                                                    stringBuilder
                                                            .append(i + 1)
                                                            .append(". ")
                                                            .append("Name : ").append(labels.get(i).getName())
                                                            .append(",")
                                                            .append("\nConfidence : ").append(labels.get(i).getConfidence().toString()).append("% .");
                                                } else {
                                                    stringBuilder
                                                            .append(i + 1)
                                                            .append(". ")
                                                            .append("Name : ").append(labels.get(i).getName())
                                                            .append(",")
                                                            .append("\nConfidence : ").append(labels.get(i).getConfidence().toString()).append("% ,");
                                                }
                                            }

                                            stringBuilder.append(". Okay, this are the detected labels from your image. If you like to perform another task simply say the task name with keyword task name.");
                                        }

                                        String imageModerationTaskMessage = stringBuilder.toString();

                                        String cardTitle = "Image Moderation Task Message";

                                        String imageModerationTaskRePromptMessage = "If you like to perform another task simply say the task name with keyword task name.";

                                        logger.debug(cardTitle + " : " + imageModerationTaskMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", imageModerationTaskMessage);
                                        session.setAttribute("repeat_re_prompt_message", imageModerationTaskRePromptMessage);

                                        return getMessageWithSimpleCardResponse(imageModerationTaskMessage, cardTitle, imageModerationTaskRePromptMessage, true);
                                    } catch (AmazonRekognitionException e) {
                                        String imageModerationTaskErrorMessage = "Unfortunately, i could not perform the image moderation task on your image now. " +
                                                "Because i could not find your image or may be some error has been occurred. " +
                                                "And also the task has been terminated. " +
                                                "Sorry for that. " +
                                                "Okay, please say a another task name to start a new task with the keyword task name. ";

                                        String cardTitle = "Image Moderation Task Error Message";

                                        String imageModerationTaskErrorRePromptMessage = "Okay, please say a another task name to start a new task with the keyword task name. ";

                                        logger.debug(cardTitle + " : " + imageModerationTaskErrorMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", imageModerationTaskErrorMessage);
                                        session.setAttribute("repeat_re_prompt_message", imageModerationTaskErrorRePromptMessage);

                                        return getMessageWithSimpleCardResponse(imageModerationTaskErrorMessage, cardTitle, imageModerationTaskErrorRePromptMessage, true);
                                    }
                                }
                                case "image object and scene detection": {
                                    String image = fileName + "." + fileFormat;

                                    AmazonRekognition rekognitionClient = getAmazonRekognitionClient();

                                    DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withS3Object(new S3Object().withName(image).withBucket(bucketName)));

                                    try {
                                        StringBuilder stringBuilder = new StringBuilder();

                                        DetectLabelsResult result = rekognitionClient.detectLabels(request);

                                        List<Label> labels = result.getLabels();

                                        stringBuilder.append("Okay image object and scene detection task is useful to detects automatically labels objects, concepts and scenes in your image, and provides a confidence score. I completed the analysis on your image ").append(image);

                                        if (labels.size() <= 0) {
                                            stringBuilder.append("I could not find any object are scene in your image. ")
                                                    .append("Okay, if you like to perform another task simply say the task name with keyword task name.");
                                        } else {
                                            stringBuilder.append(". Now, i give the name of the labels,parent name and also their confidence level. ");

                                            for (int i = 0; i < labels.size(); i++) {
                                                if (i == labels.size() - 1) {
                                                    stringBuilder
                                                            .append(i + 1)
                                                            .append(". ")
                                                            .append("Name : ").append(labels.get(i).getName())
                                                            .append(",")
                                                            .append("\nConfidence : ").append(labels.get(i).getConfidence().toString()).append("% .");

                                                } else {
                                                    stringBuilder
                                                            .append(i + 1)
                                                            .append(". ")
                                                            .append("Name : ").append(labels.get(i).getName())
                                                            .append(",")
                                                            .append("\nConfidence : ").append(labels.get(i).getConfidence().toString()).append("% ,");
                                                }

                                                stringBuilder.append("Parent Names : ");

                                                List<Parent> parents = labels.get(i).getParents();

                                                if (!parents.isEmpty()) {
                                                    for (int j = 0; j < parents.size(); j++) {
                                                        if (j == parents.size() - 1) {
                                                            stringBuilder.append(j + 1).append(". ").append(parents.get(j).getName()).append(". ");
                                                        } else {
                                                            stringBuilder.append(j + 1).append(". ").append(parents.get(j).getName()).append(", ");
                                                        }
                                                    }
                                                }
                                            }

                                            stringBuilder.append(". Okay, this are the detected labels from your image. If you like to perform another task simply say the task name with keyword task name.");
                                        }

                                        String imageObjectAndSceneDetectionTaskMessage = stringBuilder.toString();

                                        String cardTitle = "Image Object And Scene Detection Task Message";

                                        String imageObjectAndSceneDetectionTaskRePromptMessage = "If you like to perform another task simply say the task name with keyword task name.";

                                        logger.debug(cardTitle + " : " + imageObjectAndSceneDetectionTaskMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", imageObjectAndSceneDetectionTaskMessage);
                                        session.setAttribute("repeat_re_prompt_message", imageObjectAndSceneDetectionTaskRePromptMessage);

                                        return getMessageWithSimpleCardResponse(imageObjectAndSceneDetectionTaskMessage, cardTitle, imageObjectAndSceneDetectionTaskRePromptMessage, true);
                                    } catch (AmazonRekognitionException e) {
                                        String imageObjectAndSceneDetectionTaskMessage = "Unfortunately, i could not perform the image object and scene detection task on your image now. " +
                                                "Because i could not find your image or may be some error has been occurred. " +
                                                "And also the task has been terminated. " +
                                                "Sorry for that. " +
                                                "Okay, please say a another task name to start a new task with the keyword task name. ";

                                        String cardTitle = "Image Object And Scene Detection Task Error Message";

                                        String imageObjectAndSceneDetectionTaskRePromptMessage = "Okay, please say a another task name to start a new task with the keyword task name. ";

                                        logger.debug(cardTitle + " : " + imageObjectAndSceneDetectionTaskMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", imageObjectAndSceneDetectionTaskMessage);
                                        session.setAttribute("repeat_re_prompt_message", imageObjectAndSceneDetectionTaskRePromptMessage);

                                        return getMessageWithSimpleCardResponse(imageObjectAndSceneDetectionTaskMessage, cardTitle, imageObjectAndSceneDetectionTaskRePromptMessage, true);
                                    }
                                }
                                case "image face comparison": {
                                    String imageFaceComparisonMessage = "Okay, you choose image face comparison task. " +
                                            "This task is useful to compare faces to see how closely they match based on a similarity percentage. " +
                                            "If you want to perform this task, we want the target image to compare. " +
                                            "So, please say the target image name with file format in the same bucket . " + bucketName +
                                            "Okay, now first say the target file name with the keyword target file name. " +
                                            "Example, target file name my file.";

                                    String cardTitle = "Image Face Comparison Task Message";

                                    String imageFaceComparisonRePromptMessage = "Okay, now first say the target file name with the keyword target file name. " +
                                            "Example, target file name my file.";

                                    logger.debug(cardTitle + " : " + imageFaceComparisonMessage);

                                    session.setAttribute("task_name", taskName);
                                    session.setAttribute("bucket_name", bucketName);
                                    session.setAttribute("file_name", fileName);
                                    session.setAttribute("file_format", fileFormat);
                                    session.setAttribute("repeat_message", imageFaceComparisonMessage);
                                    session.setAttribute("repeat_re_prompt_message", imageFaceComparisonRePromptMessage);

                                    return getMessageWithSimpleCardResponse(imageFaceComparisonMessage, cardTitle, imageFaceComparisonRePromptMessage, true);
                                }
                                case "image text detection": {
                                    String image = fileName + "." + fileFormat;

                                    AmazonRekognition rekognitionClient = getAmazonRekognitionClient();

                                    DetectTextRequest request = new DetectTextRequest()
                                            .withImage(new Image().withS3Object(new S3Object().withName(image).withBucket(bucketName)));

                                    try {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        StringBuilder paragraphText = new StringBuilder();

                                        DetectTextResult result = rekognitionClient.detectText(request);
                                        List<TextDetection> textDetections = result.getTextDetections();

                                        stringBuilder.append("Okay, image text detection task is useful extract text from your image. I completed the analysis on your image ").append(image).append(". Now, i say the extracted text from the image. ").append("\n");

                                        for (TextDetection text : textDetections) {
                                            if (text.getType().equals("LINE")) {
                                                stringBuilder.append(text.getDetectedText()).append(" ");
                                                paragraphText.append(text.getDetectedText()).append(" ");
                                            }
                                        }

                                        stringBuilder.append(". Okay, this are the detected texts from your image. " +
                                                "Okay, now i can do three type of tasks with paragraph text. " +
                                                "First one is text translate, second one is text key phrase detection, third one is get definition for any word from the detedcted text paragraph. " +
                                                "Okay, if you like to perform any of the above task, simply say yes i like to perform the task. " +
                                                "Otherwise, say no i don't like to perform the task. ");

                                        String imageTextDetectionTaskMessage = stringBuilder.toString();

                                        String cardTitle = "Image Text Detection Task Message";

                                        String imageTextDetectionTaskRePromptMessage = "Okay, if you like to perform any of the above task, simply say yes i like to perform the task. " +
                                                "Otherwise, say no i don't like to perform the task. ";

                                        session.setAttribute("task_name", taskName);
                                        session.setAttribute("paragraph_text", paragraphText.toString());

                                        logger.debug(cardTitle + " : " + imageTextDetectionTaskMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", imageTextDetectionTaskMessage);
                                        session.setAttribute("repeat_re_prompt_message", imageTextDetectionTaskRePromptMessage);

                                        return getMessageWithSimpleCardResponse(imageTextDetectionTaskMessage, cardTitle, imageTextDetectionTaskRePromptMessage, true);
                                    } catch (AmazonRekognitionException e) {
                                        String imageTextDetectionTaskMessage = "Unfortunately, i could not perform the image text detection task on your image now. " +
                                                "Because i could not find your image or may be some error has been occurred. " +
                                                "And also the task has been terminated. " +
                                                "Sorry for that. " +
                                                "Okay, please say a another task name to start a new task with the keyword task name. ";

                                        String cardTitle = "Image Text Detection Task Error Message";

                                        String imageTextDetectionTaskRePromptMessage = "Okay, please say a another task name to start a new task with the keyword task name. ";

                                        logger.debug(cardTitle + " : " + imageTextDetectionTaskMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", imageTextDetectionTaskMessage);
                                        session.setAttribute("repeat_re_prompt_message", imageTextDetectionTaskRePromptMessage);

                                        return getMessageWithSimpleCardResponse(imageTextDetectionTaskMessage, cardTitle, imageTextDetectionTaskRePromptMessage, true);
                                    }
                                }
                                case "audio extract text": {
                                    String audioTextExtractMessage;

                                    String cardTitle;

                                    String audioTextExtractRePromptMessage;

                                    StringBuilder stringBuilder = new StringBuilder();

                                    String image = fileName + "." + fileFormat;

                                    String url = "https://s3.amazonaws.com/" + bucketName + "/" + fileName + "." + fileFormat;

                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_SSS");

                                    String transcriptionJobName = dateFormat.format(System.currentTimeMillis());

                                    AmazonTranscribe transcribeClient = getAmazonTranscribeClient();

                                    try {
                                        StartTranscriptionJobRequest jobRequest = new StartTranscriptionJobRequest();

                                        Media media = new Media();

                                        media.setMediaFileUri(url);

                                        jobRequest
                                                .withMediaFormat(fileFormat)
                                                .withLanguageCode(LanguageCode.EnUS)
                                                .withMedia(media)
                                                .setTranscriptionJobName(transcriptionJobName);

                                        transcribeClient.startTranscriptionJob(jobRequest);

                                        GetTranscriptionJobRequest getTranscriptionJobRequest = new GetTranscriptionJobRequest();

                                        getTranscriptionJobRequest.setTranscriptionJobName(transcriptionJobName);

                                        TranscriptionJob transcriptionJob;

                                        while (true) {
                                            transcriptionJob = transcribeClient.getTranscriptionJob(getTranscriptionJobRequest).getTranscriptionJob();

                                            if (transcriptionJob.getTranscriptionJobStatus().equals(TranscriptionJobStatus.COMPLETED.name())) {
                                                HttpResponse response = HttpRequest.get(transcriptionJob.getTranscript().getTranscriptFileUri()).send();

                                                String result = response.charset("UTF-8").bodyText();

                                                TranscriptionModel transcriptionModel = new Gson().fromJson(result, TranscriptionModel.class);

                                                stringBuilder
                                                        .append("Okay, audio text extract task is useful extract text from your image. I completed the analysis on your audio file ")
                                                        .append(image)
                                                        .append(". Now, i say the extracted text from the audio file. ");

                                                for (TranscriptionResultsTranscripts transcripts : transcriptionModel.getResults().getTranscripts()) {
                                                    stringBuilder
                                                            .append(transcripts.getTranscript());
                                                }

                                                stringBuilder.append(". Okay, this are the detected texts from your audio file. " +
                                                        "Okay, now i can do three type of tasks with paragraph text. " +
                                                        "First one is text translate, second one is text key phrase detection, third one is get definition for any word from the detected text paragraph. " +
                                                        "Okay, if you like to perform any of the above task, simply say yes i like to perform the task. " +
                                                        "Otherwise, say no i don't like to perform the task. ");

                                                audioTextExtractMessage = stringBuilder.toString();

                                                cardTitle = "Audio Text Extract Task Message";

                                                audioTextExtractRePromptMessage = "Okay, if you like to perform any of the above task, simply say yes i like to perform the task. " +
                                                        "Otherwise, say no i don't like to perform the task. ";

                                                logger.debug(cardTitle + " : " + audioTextExtractMessage);

                                                session.setAttribute("task_name", taskName);
                                                session.setAttribute("bucket_name", bucketName);
                                                session.setAttribute("file_name", fileName);
                                                session.setAttribute("file_format", fileFormat);
                                                session.setAttribute("repeat_message", audioTextExtractMessage);
                                                session.setAttribute("repeat_re_prompt_message", audioTextExtractRePromptMessage);

                                                break;
                                            } else if (transcriptionJob.getTranscriptionJobStatus().equals(TranscriptionJobStatus.IN_PROGRESS.name())) {
                                                System.out.print(transcriptionJob.getTranscriptionJobStatus());
                                            } else {
                                                stringBuilder.append("Unfortunately, i could not perform the audio extract text task on your audio file now. Because, i could not find your audio file or audio file format is invalid or the file size is large. Please, use valid file formats and small file size only. And also the task has been terminated. So, please start a new task. Sorry for that. Okay, please say a another task name with the keyword task name. ");

                                                audioTextExtractMessage = stringBuilder.toString();

                                                cardTitle = "Audio Text Extract Task Errors Message";

                                                audioTextExtractRePromptMessage = " Okay, please say a another task name with the keyword task name. ";

                                                logger.debug(cardTitle + " : " + audioTextExtractMessage);

                                                session.removeAttribute("task_name");
                                                session.removeAttribute("bucket_name");
                                                session.removeAttribute("file_name");
                                                session.removeAttribute("file_format");
                                                session.setAttribute("repeat_message", audioTextExtractMessage);
                                                session.setAttribute("repeat_re_prompt_message", audioTextExtractRePromptMessage);

                                                break;
                                            }

                                            synchronized (this) {
                                                try {
                                                    this.wait(500);
                                                } catch (InterruptedException e) {
                                                    audioTextExtractMessage = "Unfortunately, i could not perform the audio extract text task on your audio file now. " +
                                                            "Because the file is large in size. " +
                                                            "So, please use small size files between 0 to 20 mega bytes" +
                                                            "And also the task has been terminated. " +
                                                            "So, please start a new task. " +
                                                            "Okay, if you want start a another task simply say the task name with the keyword task name. ";

                                                    cardTitle = "Audio Extract Text Task Error Message";

                                                    audioTextExtractRePromptMessage = "Okay, if you want start a another task simply say the task name with the keyword task name. ";

                                                    logger.debug(cardTitle + " : " + audioTextExtractMessage);

                                                    session.removeAttribute("task_name");
                                                    session.removeAttribute("bucket_name");
                                                    session.removeAttribute("file_name");
                                                    session.removeAttribute("file_format");
                                                    session.setAttribute("repeat_message", audioTextExtractMessage);
                                                    session.setAttribute("repeat_re_prompt_message", audioTextExtractRePromptMessage);

                                                    break;
                                                }
                                            }
                                        }

                                        return getMessageWithSimpleCardResponse(audioTextExtractMessage, cardTitle, audioTextExtractRePromptMessage, true);
                                    } catch (AmazonTranscribeException e) {
                                        String audioExtractTextTaskMessage = "Unfortunately, i could not perform the audio extract text task on your audio file now. " +
                                                "Because, i could not find your audio file or audio file format is invalid or the file size is large. " +
                                                "Please, use valid file formats and small file size only. " +
                                                "And also the task has been terminated. " +
                                                "So, please start a new task. " +
                                                "Okay, if you want start a another task simply say the task name with the keyword task name. ";

                                        String cardTitleError = "Audio Extract Text Task Error Message";

                                        String audioExtractTextTaskRePromptMessage = "Okay, if you want start a another task simply say the task name with the keyword task name. ";

                                        logger.debug(cardTitleError + " : " + audioExtractTextTaskMessage);

                                        session.removeAttribute("task_name");
                                        session.removeAttribute("bucket_name");
                                        session.removeAttribute("file_name");
                                        session.removeAttribute("file_format");
                                        session.setAttribute("repeat_message", audioExtractTextTaskMessage);
                                        session.setAttribute("repeat_re_prompt_message", audioExtractTextTaskRePromptMessage);

                                        return getMessageWithSimpleCardResponse(audioExtractTextTaskMessage, cardTitleError, audioExtractTextTaskRePromptMessage, true);
                                    }
                                }
                                default:
                                    return getTaskNameEmptyResponse(session);
                            }
                        }
                    }
                    case "ThisIsNotMyFileUrlIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("text")) {
                            return getTextMessageResponse(taskName, session);
                        } else {
                            String thisIsNotMyFileUrlMessage = "Okay, don't worry please say the bucket name,file name and file format again one by one. " +
                                    "Now first say the bucket name with the keyword bucket name. ";

                            String cardTitle = "This Is Not My File Url Message";

                            String thisIsNotMyFileUrlRePromptMessage = "Now first say the bucket name with the keyword bucket name. ";

                            logger.debug(cardTitle + " : " + thisIsNotMyFileUrlMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("repeat_message", thisIsNotMyFileUrlMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsNotMyFileUrlRePromptMessage);

                            return getMessageWithSimpleCardResponse(thisIsNotMyFileUrlMessage, cardTitle, thisIsNotMyFileUrlRePromptMessage, true);
                        }
                    }
                    case "GetTargetFileNameIntent": {
                        String targetFileName = intent.getSlot("target_file_name").getValue();

                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (!taskName.equals("image face comparison")) {
                            return getImageFaceComparisonTaskErrorResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else if (targetFileName.isEmpty()) {
                            return getTargetFileNameRecognizeEmptyResponse(taskName, bucketName, fileName, fileFormat, session);
                        } else {
                            String getTargetFileNameMessage = "Confirm " + targetFileName + " is your target file name. " +
                                    "If yes, say yes this is my target file name. " +
                                    "If no, say no this is not my target file name. ";

                            String cardTitle = "Confirm Target File Name Message";

                            logger.debug(cardTitle + " : " + getTargetFileNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("target_file_name", targetFileName);
                            session.setAttribute("repeat_message", getTargetFileNameMessage);
                            session.setAttribute("repeat_re_prompt_message", getTargetFileNameMessage);

                            return getMessageWithSimpleCardResponse(getTargetFileNameMessage, cardTitle, getTargetFileNameMessage, true);
                        }
                    }
                    case "ThisIsMyTargetFileNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        String targetFileName = getStoredSessionTargetFileName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (!taskName.equals("image face comparison")) {
                            return getImageFaceComparisonTaskErrorResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else if (targetFileName.isEmpty()) {
                            return getTargetFileNameEmptyResponse(taskName, bucketName, fileName, fileFormat, session);
                        } else {
                            String thisIsMyTargetFileNameMessage = "Okay, now say your target file format with the keyword target file format. " +
                                    "Example, target file format jpg. ";

                            String cardTitle = "This Is My Target File Name Message";

                            logger.debug(cardTitle + " : " + thisIsMyTargetFileNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("target_file_name", targetFileName);
                            session.setAttribute("repeat_message", thisIsMyTargetFileNameMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsMyTargetFileNameMessage);

                            return getMessageWithSimpleCardResponse(thisIsMyTargetFileNameMessage, cardTitle, thisIsMyTargetFileNameMessage, true);
                        }
                    }
                    case "ThisIsNotMyTargetFileNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        String targetFileName = getStoredSessionTargetFileName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (!taskName.equals("image face comparison")) {
                            return getImageFaceComparisonTaskErrorResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else if (targetFileName.isEmpty()) {
                            return getTargetFileNameEmptyResponse(taskName, bucketName, fileName, fileFormat, session);
                        } else {
                            String thisIsNotMyTargetFileNameMessage = "Okay don't worry please say the target file name again with the keyword target file name. " +
                                    "Example, target file name my second image. ";

                            String cardTitle = "This Is Not My Target File Name Message";

                            logger.debug(cardTitle + " : " + thisIsNotMyTargetFileNameMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("repeat_message", thisIsNotMyTargetFileNameMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsNotMyTargetFileNameMessage);

                            return getMessageWithSimpleCardResponse(thisIsNotMyTargetFileNameMessage, cardTitle, thisIsNotMyTargetFileNameMessage, true);
                        }
                    }
                    case "GetTargetFileFormatIntent": {
                        String targetFileFormat = intent.getSlot("target_file_format").getValue();

                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        String targetFileName = getStoredSessionTargetFileName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (!taskName.equals("image face comparison")) {
                            return getImageFaceComparisonTaskErrorResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else if (targetFileName.isEmpty()) {
                            return getTargetFileNameEmptyResponse(taskName, bucketName, fileName, fileFormat, session);
                        } else if (targetFileFormat.isEmpty()) {
                            return getTargetFileFormatRecognizeEmptyResponse(taskName, bucketName, fileName, fileFormat, targetFileName, session);
                        } else {
                            String getTargetFileFormatMessage = "Confirm " + targetFileFormat + " is your target file format. " +
                                    "If yes, say yes this is my target file format. " +
                                    "If no, say no this is not my target file format. ";

                            String cardTitle = "Confirm Target File Format Message";

                            logger.debug(cardTitle + " : " + getTargetFileFormatMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("target_file_name", targetFileName);
                            session.setAttribute("target_file_format", targetFileFormat);
                            session.setAttribute("repeat_message", getTargetFileFormatMessage);
                            session.setAttribute("repeat_re_prompt_message", getTargetFileFormatMessage);

                            return getMessageWithSimpleCardResponse(getTargetFileFormatMessage, cardTitle, getTargetFileFormatMessage, true);
                        }
                    }
                    case "ThisIsMyTargetFileFormatIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        String targetFileName = getStoredSessionTargetFileName(session);

                        String targetFileFormat = getStoredSessionTargetFileFormat(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (!taskName.equals("image face comparison")) {
                            return getImageFaceComparisonTaskErrorResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else if (targetFileName.isEmpty()) {
                            return getTargetFileNameEmptyResponse(taskName, bucketName, fileName, fileFormat, session);
                        } else if (targetFileFormat.isEmpty()) {
                            return getTargetFileFormatEmptyResponse(taskName, bucketName, fileName, fileFormat, targetFileName, session);
                        } else {
                            String targetImage = fileName + "." + fileFormat;

                            String sourceImage = targetFileName + "." + targetFileFormat;

                            StringBuilder stringBuilder = new StringBuilder();

                            AmazonRekognition rekognitionClient = getAmazonRekognitionClient();

                            try {
                                CompareFacesRequest request = new CompareFacesRequest()
                                        .withSourceImage(new Image().withS3Object(new S3Object().withName(sourceImage).withBucket(bucketName)))
                                        .withTargetImage(new Image().withS3Object(new S3Object().withName(targetImage).withBucket(bucketName)))
                                        .withSimilarityThreshold(70F);

                                CompareFacesResult result = rekognitionClient.compareFaces(request);

                                List<CompareFacesMatch> faceDetails = result.getFaceMatches();

                                if (faceDetails.size() == 0) {
                                    stringBuilder.append("I could not any found faces in your target image. Okay if you like to perform another task simply say the task name with keyword task name.");
                                } else if (faceDetails.size() == 1) {
                                    stringBuilder
                                            .append("I found ")
                                            .append(faceDetails.size()).append(" face in your target image using your source image. Now i give the confidence level of the target face, how they are closely matches with your source image face. ")
                                            .append("Confidence level of target face : ")
                                            .append(faceDetails.get(0).getSimilarity())
                                            .append(". Okay, if you like to perform another task simply say the task name with keyword task name.");

                                } else {
                                    stringBuilder
                                            .append("I found ")
                                            .append(faceDetails.size())
                                            .append(" faces in your target image using your source image. Now i give the confidence level of the target faces, how they are closely matches with your source image face. ");

                                    for (int i = 0; i < faceDetails.size(); i++) {
                                        stringBuilder.append("Confidence level of target face ").append(i + 1).append(" : ").append(faceDetails.get(i).getSimilarity());
                                    }

                                    stringBuilder.append(". Okay this are the detected faces in your target image with confidence. If you like to perform another task simply say the task name with keyword task name.");
                                }

                                String imageFaceComparisonMessage = stringBuilder.toString();

                                String cardTitle = "Image Face Comparison Message";

                                String imageFaceComparisonRePromptMessage = "If you like to perform another task simply say the task name with keyword task name.";

                                logger.debug(cardTitle + " : " + imageFaceComparisonMessage);

                                session.removeAttribute("task_name");
                                session.removeAttribute("bucket_name");
                                session.removeAttribute("file_name");
                                session.removeAttribute("file_format");
                                session.removeAttribute("target_file_name");
                                session.removeAttribute("target_file_format");
                                session.setAttribute("repeat_message", imageFaceComparisonMessage);
                                session.setAttribute("repeat_re_prompt_message", imageFaceComparisonRePromptMessage);

                                return getMessageWithSimpleCardResponse(imageFaceComparisonMessage, cardTitle, imageFaceComparisonRePromptMessage, true);
                            } catch (AmazonRekognitionException e) {
                                String imageFaceComparisonTaskMessage = "Unfortunately, i could not perform the image face comparison task on your images now. " +
                                        "Because i could not find your image or may be some error has been occurred. " +
                                        "Sorry for that. " +
                                        "So, please say a another task name with the keyword task name. ";

                                String cardTitle = "Image Face Comparison Task Error Message";

                                String imageFaceComparisonTaskRePromptMessage = "So, please say a another task name with the keyword task name. ";

                                logger.debug(cardTitle + " : " + imageFaceComparisonTaskMessage);

                                session.removeAttribute("task_name");
                                session.removeAttribute("bucket_name");
                                session.removeAttribute("file_name");
                                session.removeAttribute("file_format");
                                session.removeAttribute("target_file_name");
                                session.removeAttribute("target_file_format");
                                session.setAttribute("repeat_message", imageFaceComparisonTaskMessage);
                                session.setAttribute("repeat_re_prompt_message", imageFaceComparisonTaskRePromptMessage);

                                return getMessageWithSimpleCardResponse(imageFaceComparisonTaskMessage, cardTitle, imageFaceComparisonTaskRePromptMessage, true);
                            }
                        }
                    }
                    case "ThisIsNotMyTargetFileFormatIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String bucketName = getStoredSessionBucketName(session);

                        String fileName = getStoredSessionFileName(session);

                        String fileFormat = getStoredSessionFileFormat(session);

                        String targetFileName = getStoredSessionTargetFileName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (!taskName.equals("image face comparison")) {
                            return getImageFaceComparisonTaskErrorResponse(taskName, session);
                        } else if (bucketName.isEmpty()) {
                            return getBucketNameEmptyResponse(taskName, session);
                        } else if (fileName.isEmpty()) {
                            return getFileNameEmptyResponse(taskName, bucketName, session);
                        } else if (fileFormat.isEmpty()) {
                            return getFileFormatEmptyResponse(taskName, bucketName, fileName, session);
                        } else if (targetFileName.isEmpty()) {
                            return getTargetFileNameEmptyResponse(taskName, bucketName, fileName, fileFormat, session);
                        } else {
                            String thisIsNotMyTargetFileFormatMessage = "Okay don't worry please say the target file format again with the keyword target file format. " +
                                    "Example, target file format jpg. ";

                            String cardTitle = "This Is Not My Target File Format Message";

                            logger.debug(cardTitle + " : " + thisIsNotMyTargetFileFormatMessage);

                            session.setAttribute("task_name", taskName);
                            session.setAttribute("bucket_name", bucketName);
                            session.setAttribute("file_name", fileName);
                            session.setAttribute("file_format", fileFormat);
                            session.setAttribute("target_file_name", targetFileName);
                            session.setAttribute("repeat_message", thisIsNotMyTargetFileFormatMessage);
                            session.setAttribute("repeat_re_prompt_message", thisIsNotMyTargetFileFormatMessage);

                            return getMessageWithSimpleCardResponse(thisIsNotMyTargetFileFormatMessage, cardTitle, thisIsNotMyTargetFileFormatMessage, true);
                        }
                    }
                    case "ILikeToPerformTheTask": {
                        String taskName = getStoredSessionTaskName(session);

                        String paragraphText = getStoredSessionParagraphText(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (paragraphText.isEmpty()) {
                            return getParagraphTextEmptyResponse(taskName, session);
                        } else {
                            return getLikeToPerFormTaskResponse(taskName, paragraphText, session);
                        }
                    }
                    case "IDontLikeToPerformTheTask": {
                        String notLikeToPerformTaskMessage = "Okay, if you like to perform another task simply say the task name with keyword task name.";

                        String cardTitle = "I Not Like To Perform Task Message";

                        logger.debug(cardTitle + " : " + notLikeToPerformTaskMessage);

                        session.setAttribute("repeat_message", notLikeToPerformTaskMessage);
                        session.setAttribute("repeat_re_prompt_message", notLikeToPerformTaskMessage);

                        if (!getStoredSessionTaskName(session).isEmpty()) {
                            session.removeAttribute("task_name");
                        }

                        if (!getStoredSessionBucketName(session).isEmpty()) {
                            session.removeAttribute("bucket_name");
                        }

                        if (!getStoredSessionFileName(session).isEmpty()) {
                            session.removeAttribute("file_name");
                        }

                        if (!getStoredSessionFileFormat(session).isEmpty()) {
                            session.removeAttribute("file_format");
                        }

                        if (!getStoredSessionParagraphText(session).isEmpty()) {
                            session.removeAttribute("paragraph_text");
                        }

                        return getMessageWithSimpleCardResponse(notLikeToPerformTaskMessage, cardTitle, notLikeToPerformTaskMessage, true);
                    }
                    case "GetMoreTaskNameIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String paragraphText = getStoredSessionParagraphText(session);

                        String moreTaskName = intent.getSlot("more_task_name").getValue();

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (paragraphText.isEmpty()) {
                            return getParagraphTextEmptyResponse(taskName, session);
                        } else if (moreTaskName.isEmpty()) {
                            return getMoreTaskNameRecognizeEmptyResponse(taskName, paragraphText, session);
                        } else {
                            switch (moreTaskName) {
                                case "text translate":
                                    return getTextTranslateResponse(taskName, paragraphText, session);
                                case "text key phrase detection":
                                    return getTextKeyPhraseDetectionResponse(paragraphText, session);
                                case "text definition":
                                    String textDefinitionMessage = "Okay, say any word from your paragraph with the keyword definition word. ";

                                    String cardTitle = "Text Definition Message";

                                    logger.debug(cardTitle + " : " + textDefinitionMessage);

                                    session.setAttribute("repeat_message", textDefinitionMessage);
                                    session.setAttribute("repeat_re_prompt_message", textDefinitionMessage);

                                    return getMessageWithSimpleCardResponse(textDefinitionMessage, cardTitle, textDefinitionMessage, true);
                                default:
                                    return getMoreTaskNameErrorResponse(moreTaskName, session);
                            }
                        }
                    }
                    case "GetParagraphTextIntent": {
                        String taskName = getStoredSessionTaskName(session);

                        String paragraphText = intent.getSlot("paragraph_text").getValue();

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.startsWith("image") || taskName.startsWith("audio")) {
                            return getParagraphTextErrorResponse(taskName, session);
                        } else if (paragraphText.isEmpty()) {
                            return getParagraphTextRecognizeEmptyResponse(taskName, session);
                        } else if (taskName.equals("text translate")) {
                            return getTextTranslateResponse(taskName, paragraphText, session);
                        } else if (taskName.equals("text key phrase detection")) {
                            return getTextKeyPhraseDetectionResponse(paragraphText, session);
                        } else {
                            return getTaskNameEmptyResponse(session);
                        }
                    }
                    case "GetTargetLanguage": {
                        String taskName = getStoredSessionTaskName(session);

                        String paragraphText = getStoredSessionParagraphText(session);

                        String targetLanguage = intent.getSlots().get("target_language").getValue().toLowerCase();

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (taskName.equals("image object and scene detection") || taskName.equals("image face comparison") || taskName.equals("image moderation") || taskName.equals("text key phrase detection") || taskName.equals("text definition")) {
                            return getTargetLanguageErrorResponse(session);
                        } else if (paragraphText.isEmpty()) {
                            return getParagraphTextEmptyResponse(taskName, session);
                        } else if (targetLanguage.isEmpty()) {
                            return getTargetLanguageRecognizeEmptyResponse(taskName, paragraphText, session);
                        } else {
                            String targetLanguageCode = getLanguageCode(targetLanguage);

                            AmazonTranslate translateClient = getAmazonTranslateClient();

                            TranslateTextRequest request = new TranslateTextRequest()
                                    .withText(paragraphText)
                                    .withSourceLanguageCode("en")
                                    .withTargetLanguageCode(targetLanguageCode);

                            TranslateTextResult result = translateClient.translateText(request);

                            String getTargetLanguageMessage = "Okay, i translate your paragraph text into " + targetLanguage +
                                    ". Now i say the translated text, " +
                                    result.getTranslatedText() +
                                    ". Okay, if you like to perform another task simply say the task name with keyword task name. ";

                            String cardTitle = "Target Language Message";

                            String getTargetLanguageRePromptMessage = "Okay, if you like to perform another task simply say the task name with keyword task name. ";

                            session.removeAttribute("task_name");
                            session.removeAttribute("paragraph_text");
                            session.removeAttribute("target_language");
                            session.removeAttribute("bucket_name");
                            session.removeAttribute("file_name");
                            session.removeAttribute("file_format");

                            return getMessageWithSimpleCardResponse(getTargetLanguageMessage, cardTitle, getTargetLanguageRePromptMessage, true);
                        }
                    }
                    case "GetDefinitionWordIntent": {
                        String definitionWord = intent.getSlot("definition_word").getValue();

                        String taskName = getStoredSessionTaskName(session);

                        if (taskName.isEmpty()) {
                            return getTaskNameEmptyResponse(session);
                        } else if (definitionWord.isEmpty()) {
                            return getDefinitionWordRecognizeEmptyResponse(taskName, session);
                        } else {
                            return getDefinitionForWord(definitionWord, session);
                        }
                    }
                    case "AMAZON.HelpIntent": {
                        String helpMessage = "Hi, It's a pleasure to help to you. " +
                                "My work is to analyse the image files, audio files and voice texts. " +
                                "And then i can perform some tasks on your files and voice texts. " +
                                "And after finishing the task, i will say the results of task. " +
                                "Okay, i listed below what i will can do with your files and voice texts. " +
                                "I listed below the task names. " +
                                "Your work is to choose one task name from list and say it to me with the keyword task name. " +
                                "Example, task name image moderation. " +
                                "Okay, now i say the task names one by one, first one is image moderation, " +
                                "second one is image object and scene detection, " +
                                "third one is image text detection, " +
                                "fourth one is image face comparison, " +
                                "fifth one is audio extract text, " +
                                "sixth one is text translate, " +
                                "seventh one is text key phrase detection, " +
                                "and finally eight one is text definition. " +
                                "Okay, this are tasks i will do on your files and voice texts. " +
                                "Now, choose one task name from above list. " +
                                "And say the task name with keyword task name. ";

                        String cardTitle = "Help Message";

                        String helpRePromptMessage = "Now, choose one task name from above list. " +
                                "And say the task name with keyword task name. ";

                        logger.debug(cardTitle + " : " + helpMessage);

                        session.setAttribute("repeat_message", helpMessage);
                        session.setAttribute("repeat_re_prompt_message", helpRePromptMessage);

                        return getMessageWithSimpleCardResponse(helpMessage, cardTitle, helpRePromptMessage, true);
                    }
                    case "AMAZON.RepeatIntent": {
                        String repeatMessage = intent.getSlot("repeat_message").getValue();

                        String repeatRePromptMessage = intent.getSlot("repeat_re_prompt_message").getValue();

                        String cardTitle = "Repeat Message";

                        logger.debug(cardTitle + " : " + repeatMessage);

                        session.setAttribute("repeat_message", repeatMessage);
                        session.setAttribute("repeat_re_prompt_message", repeatRePromptMessage);

                        return getMessageWithSimpleCardResponse(repeatMessage, cardTitle, repeatRePromptMessage, true);
                    }
                    case "AMAZON.FallbackIntent":
                        return getFallbackResponse(session);
                    case "AMAZON.StopIntent":
                        return getStopOrCancelResponse(session);
                    case "AMAZON.CancelIntent":
                        return getStopOrCancelResponse(session);
                    case "AMAZON.YesIntent":
                        return getYesResponse(session);
                    case "AMAZON.NoIntent":
                        return getNoResponse(session);
                    default:
                        return getFallbackResponse(session);
                }
            }
            else
            {
                return getFallbackResponse(session);
            }
        }
        else
        {
            return getFallbackResponse(session);
        }
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> speechletRequestEnvelope)
    {
        logger.debug("Session Ended at : " + speechletRequestEnvelope.getRequest().getTimestamp());
    }

    // Get Definition for word

    private SpeechletResponse getDefinitionForWord(String word, Session session)
    {
        try
        {
            Document document = Jsoup.connect("https://en.wikipedia.org/wiki/" + word).get();

            Elements paragraphs = document.select("p:not(:has(#coordinates))");

            if (paragraphs.size() > 2)
            {
                Element first = paragraphs.get(1);

                String definition = first.text();

                if (definition != null && !definition.equals("null") && !definition.equals(""))
                {
                    return getWordDefinitionSuccessResponse(word,definition,session);
                }
                else
                {
                    return getWordDefinitionFailureResponse(word,session);
                }
            }
            else
            {
                SearchQuery query = new SearchQuery.Builder(word).site("en.wikipedia.org").numResults(2).build();

                SearchResult result = new GoogleWebSearch().search(query);

                List<String> url = result.getUrls();

                if (url != null && url.size() > 0)
                {
                    Document finalDocument = Jsoup.connect(url.get(0)).get();

                    Elements finalParagraphs = finalDocument.select("p:not(:has(#coordinates))");

                    if (finalParagraphs.size() > 2)
                    {
                        Element firstParagraph = finalParagraphs.get(1);

                        String definition = firstParagraph.text();

                        if (definition != null && !definition.equals("null") && !definition.equals(""))
                        {
                            return getWordDefinitionSuccessResponse(word,definition,session);
                        }
                        else
                        {
                            return getWordDefinitionFailureResponse(word,session);
                        }
                    }
                    else
                    {
                        return getWordDefinitionFailureResponse(word,session);
                    }
                }
                else
                {
                    return getWordDefinitionFailureResponse(word,session);
                }
            }
        }
        catch (IOException e)
        {
            return getWordDefinitionFailureResponse(word,session);
        }
    }

    // Get Language Code

    private String getLanguageCode(String language)
    {
        Map<String,String> languageCodes = new HashMap<>();

        languageCodes.put("turkish","tr");
        languageCodes.put("swedish","sv");
        languageCodes.put("spanish","es");
        languageCodes.put("russian","ru");
        languageCodes.put("portuguese","pt");
        languageCodes.put("polish","pl");
        languageCodes.put("korean","ko");
        languageCodes.put("japanese","ja");
        languageCodes.put("italian","it");
        languageCodes.put("indonesian","id");
        languageCodes.put("hebrew","he");
        languageCodes.put("german","de");
        languageCodes.put("french","fr");
        languageCodes.put("finnish","fi");
        languageCodes.put("dutch","nl");
        languageCodes.put("danish","da");
        languageCodes.put("czech","cs");
        languageCodes.put("chinese traditional","zh-TW");
        languageCodes.put("chinese","zh");
        languageCodes.put("arabic","ar");

        return languageCodes.get(language);
    }

    //Amazon Client Credentials

    private AmazonRekognition getAmazonRekognitionClient()
    {
        return AmazonRekognitionClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials()
                {
                    @Override
                    public String getAWSAccessKeyId()
                    {
                        return "AKIAINP5QUYC67NNSFUA";
                    }

                    @Override
                    public String getAWSSecretKey()
                    {
                        return "CfmsXdlwrsrsqqkAuEQCOP8/CgEhkAfiTIhOnrpJ";
                    }
                }))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    private AmazonTranscribe getAmazonTranscribeClient()
    {
        return AmazonTranscribeClient.builder()
                .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials()
                {
                    @Override
                    public String getAWSAccessKeyId()
                    {
                        return "AKIAINP5QUYC67NNSFUA";
                    }

                    @Override
                    public String getAWSSecretKey()
                    {
                        return "CfmsXdlwrsrsqqkAuEQCOP8/CgEhkAfiTIhOnrpJ";
                    }
                }))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    private AmazonTranslate getAmazonTranslateClient()
    {
        return AmazonTranslateClient.builder()
                .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials()
                {
                    @Override
                    public String getAWSAccessKeyId()
                    {
                        return "AKIAINP5QUYC67NNSFUA";
                    }

                    @Override
                    public String getAWSSecretKey()
                    {
                        return "CfmsXdlwrsrsqqkAuEQCOP8/CgEhkAfiTIhOnrpJ";
                    }
                }))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    private AmazonComprehend getAmazonComprehendClient()
    {
        return AmazonComprehendClient.builder()
                .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials()
                {
                    @Override
                    public String getAWSAccessKeyId()
                    {
                        return "AKIAINP5QUYC67NNSFUA";
                    }

                    @Override
                    public String getAWSSecretKey()
                    {
                        return "CfmsXdlwrsrsqqkAuEQCOP8/CgEhkAfiTIhOnrpJ";
                    }
                }))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    //Get Session Attributes

    private String getStoredSessionTaskName(Session session)
    {
        String taskName = (String) session.getAttribute("task_name");

        if (taskName != null)
        {
            return taskName;
        }
        else
        {
            return "";
        }
    }

    private String getStoredSessionBucketName(Session session)
    {
        String filePath = (String) session.getAttribute("bucket_name");

        if (filePath != null)
        {
            return filePath;
        }
        else
        {
            return "";
        }
    }

    private String getStoredSessionFileName(Session session)
    {
        String fileName = (String) session.getAttribute("file_name");

        if (fileName != null)
        {
            return fileName;
        }
        else
        {
            return "";
        }
    }

    private String getStoredSessionFileFormat(Session session)
    {
        String fileFormat = (String) session.getAttribute("file_format");

        if (fileFormat != null)
        {
            return fileFormat;
        }
        else
        {
            return "";
        }
    }

    private String getStoredSessionTargetFileName(Session session)
    {
        String targetFileName = (String) session.getAttribute("target_file_name");

        if (targetFileName != null)
        {
            return targetFileName;
        }
        else
        {
            return "";
        }
    }

    private String getStoredSessionTargetFileFormat(Session session)
    {
        String targetFileFormat = (String) session.getAttribute("target_file_format");

        if (targetFileFormat != null)
        {
            return targetFileFormat;
        }
        else
        {
            return "";
        }
    }

    private String getStoredSessionParagraphText(Session session)
    {
        String paragraphText = (String) session.getAttribute("paragraph_text");

        if (paragraphText != null)
        {
            return paragraphText;
        }
        else
        {
            return "";
        }
    }

    //IntentResponses

    private SpeechletResponse getStorageHelperIntentResponse(Session session)
    {
        String storageHelperMessage = "Okay, s3 storage is a cloud storage of amazon aws. " +
                "Okay, i say a steps to how to store your files in amazon aws s3 storage. " +
                "First step, go to amazon aws and create your account. " +
                "And then second step is, type s3 in the search box and choose s3 (scalable storage in the cloud). " +
                "And then third step is, click create bucket and type your bucket name and choose a region in the name and region tab." +
                "And then click next and in the configure options tab uncheck all checked options. " +
                "And click next and set manage system permissions as 'grant amazon s3 log delivery group write access to this bucket' in the set permissions tab. " +
                "And click next and click create bucket. " +
                "Now your bucket has been created. " +
                "Click on your bucket name and click upload, choose your file from your laptop or pc. " +
                "And set manage public permission as 'grant public read access to this object(s)' in the set permissions tab. "+
                "And click next and again click next and click upload. " +
                "Once your file is uploaded get the url of the storage path by clicking on the file name. " +
                "Once you got bucket name,file name and file format come back and say it to me. " +
                "If you don't know, how to say a bucket name,file name and file format to me. " +
                "Simply, say ' file helper '. " +
                "Otherwise, say the task name with the keyword task name";

        String cardTitle = "Storage Helper Card";

        String storageHelperRePromptMessage = "Otherwise, say the task name with the keyword task name";

        logger.debug(cardTitle + " : " + storageHelperMessage);

        session.setAttribute("repeat_message",storageHelperMessage);
        session.setAttribute("repeat_re_prompt_message",storageHelperRePromptMessage);

        return getMessageWithSimpleCardResponse(storageHelperMessage,cardTitle,storageHelperRePromptMessage,true);
    }

    private SpeechletResponse getFileHelperIntentResponse(Session session)
    {
        String filePathMessage = "Okay, the file path url of your file will be like ' https://s3.amazonaws.com/imagefiber/L1.PNG '. " +
                "Here 'https://s3.amazonaws.com' is a host name of the web page. " +
                "You don't say the host name, because i can automatically recognize the host name. " +
                "Only say the bucket name,file name and file format without the hostname and the forward slashes. " +
                "For example, i ask : say the bucket name. " +
                "Now you say the bucket name with the keyword bucket name. " +
                "Example \"bucket name 'name of your bucket'\", here bucket name is imagefiber. " +
                "And i ask : say the file name.  " +
                "Now you say the file name with the keyword file name. " +
                "Example \"filename 'name of your file name'\", here file name is L1. " +
                "And then finally i ask the file format. " +
                "Now you say the format of your file with the keyword file format. " +
                "Example \"file format 'format of your file'\", here format is PNG. " +
                "I hope you understand the instructions. " +
                "And also another thing, here is supports fue file formats only. " +
                "That file formats are png and jpg or jpeg in image files. " +
                "And mp3,mp4,wav and flac in audio files. " +
                "Now you ready to say the file path url, simply say the task name with the keyword task name";

        String cardTitle = "File Helper Message";

        String filePathRePromptMessage =  "Now you ready to say the file path url, simply say the task name with the keyword task name";

        logger.debug(cardTitle + " : " + filePathMessage);

        session.setAttribute("repeat_message",filePathMessage);
        session.setAttribute("repeat_re_prompt_message",filePathRePromptMessage);

        return getMessageWithSimpleCardResponse(filePathMessage,cardTitle,filePathRePromptMessage,true);
    }

    private SpeechletResponse getFileTypeTaskResponse(String taskName,Session session)
    {
        String fileTypeTaskMessage = "Okay you choose " + taskName + " task. " +
                "And also you choose file type task, that means we want some storage place to store and retrieve the image files called s3 storage. " +
                "I hope you already know the bucket name,file name and file format. " +
                "Or, if you don't know. " +
                "Don't worry simply say ' storage helper ' to get the bucket name,file name and file format. " +
                "And then another important thing i support only jpg and png file formats. " +
                "So, please choose jpg and png images only from your storage. " +
                "Now, you know how to say the bucket name,file name and file format to me, simply say the bucket name with the keyword bucket name. " +
                "Example, bucket name my bucket. " +
                "Otherwise if you don't how to say the bucket name,file name and file format to me. " +
                "Don't worry simply say ' file helper '. " +
                "Okay, now say the bucket name with keyword bucket name. ";

        String cardTitle = "File Type Task Message";

        String fileTypeTaskRePromptMessage = "Okay, now say the bucket name with keyword bucket name. ";

        logger.debug(cardTitle + " : " + fileTypeTaskMessage);

        session.setAttribute("task_name", taskName);
        session.setAttribute("repeat_message",fileTypeTaskMessage);
        session.setAttribute("repeat_re_prompt_message",fileTypeTaskRePromptMessage);

        return getMessageWithSimpleCardResponse(fileTypeTaskMessage, cardTitle, fileTypeTaskRePromptMessage, true);
    }

    private SpeechletResponse getTextMessageResponse(String taskName,Session session)
    {
        String textMessage = "You choose " + taskName + " task. " +
                "So, please say the text to perform the " + taskName + " task with the keyword " + taskName + ". " +
                "Or if you want to perform another task, please say the task name with the keyword task name. ";

        String cardTitle = "Text Message";

        String textRePromptMessage = "So, please say the text to perform the " + taskName + " task. " +
                "Or if you want to perform another task, please say the task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + textMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",textMessage);
        session.setAttribute("repeat_re_prompt_message",textRePromptMessage);

        return getMessageWithSimpleCardResponse(textMessage,cardTitle,textRePromptMessage,true);
    }

    private SpeechletResponse getParagraphTextErrorResponse(String taskName, Session session)
    {
        String paragraphTextErrorMessage = "You choose " + taskName + " task. " +
                "And also it is file type task. " +
                "So, please say the bucket name to perform the " + taskName + " task with keyword bucket name. " +
                "Or if you want to perform another task, please say the task name with the keyword task name. ";

        String cardTitle = "Paragraph Text Error Message";

        String paragraphTextErrorRePromptMessage = "So, please say the bucket name to perform the " + taskName + " task with keyword bucket name. " +
                "Or if you want to perform another task, please say the task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + paragraphTextErrorMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",paragraphTextErrorMessage);
        session.setAttribute("repeat_re_prompt_message",paragraphTextErrorRePromptMessage);

        return getMessageWithSimpleCardResponse(paragraphTextErrorMessage,cardTitle,paragraphTextErrorRePromptMessage,true);
    }

    private SpeechletResponse getTargetLanguageErrorResponse(Session session)
    {
        String targetLangErrorMessage = "Target language is only for text type task. " +
                "And also target language is to get the language for convert the paragraph text. " +
                "So, please say valid task name with the keyword task name. ";

        String cardTitle = "Target Language Error Message";

        String targetLangErrorRePromptMessage = "So, please say valid task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + targetLangErrorMessage);

        session.removeAttribute("task_name");
        session.setAttribute("repeat_message",targetLangErrorMessage);
        session.setAttribute("repeat_re_prompt_message",targetLangErrorRePromptMessage);

        return getMessageWithSimpleCardResponse(targetLangErrorMessage,cardTitle,targetLangErrorRePromptMessage,true);
    }

    private SpeechletResponse getImageFaceComparisonTaskErrorResponse(String taskName, Session session)
    {
        String imageFaceComparisonMessage = "You choose " + taskName + " task. " +
                "So, please say the target bucket name to perform the " + taskName + " task with keyword target bucket name. " +
                "Or if you want to perform another task, please say the task name with the keyword task name. ";

        String cardTitle = "Image Face Comparison Message";

        String imageFaceComparisonRePromptMessage = "So, please say the target bucket name to perform the " + taskName + " task with keyword target bucket name. " +
                "Or if you want to perform another task, please say the task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + imageFaceComparisonMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",imageFaceComparisonMessage);
        session.setAttribute("repeat_re_prompt_message",imageFaceComparisonRePromptMessage);

        return getMessageWithSimpleCardResponse(imageFaceComparisonMessage,cardTitle,imageFaceComparisonRePromptMessage,true);
    }

    private SpeechletResponse getTaskNameErrorResponse(String taskName, Session session)
    {
        String taskNameErrorMessage = "Sorry, i could not find the " + taskName + " task. " +
                "Please, say valid task name with the keyword task name. ";

        String cardTitle = "Task Name Error Message";

        String taskNameErrorRePromptMessage = "Please, say valid task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + taskNameErrorMessage);

        session.setAttribute("repeat_message",taskNameErrorMessage);
        session.setAttribute("repeat_re_prompt_message",taskNameErrorRePromptMessage);

        return getMessageWithSimpleCardResponse(taskNameErrorMessage,cardTitle,taskNameErrorRePromptMessage,true);
    }

    private SpeechletResponse getMoreTaskNameErrorResponse(String moreTaskName, Session session)
    {
        String moreTaskNameErrorMessage = "Sorry, i could not find the " + moreTaskName + " task. " +
                "Please, say valid task name with the keyword more task name. ";

        String cardTitle = "More Task Name Error Message";

        String moreTaskNameErrorRePromptMessage = "Please, say valid task name with the keyword more task name. ";

        logger.debug(cardTitle + " : " + moreTaskNameErrorMessage);

        session.setAttribute("repeat_message",moreTaskNameErrorMessage);
        session.setAttribute("repeat_re_prompt_message",moreTaskNameErrorRePromptMessage);

        return getMessageWithSimpleCardResponse(moreTaskNameErrorMessage,cardTitle,moreTaskNameErrorRePromptMessage,true);
    }

    private SpeechletResponse getTaskNameRecognizeEmptyResponse(Session session)
    {
        String taskNameRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the task name again with the keyword task name. ";

        String cardTitle = "Task Name Recognize Empty Message";

        String taskNameRecognizeEmptyRePromptMessage = "Please say the task name again with keyword task name. ";

        logger.debug(cardTitle + " : " + taskNameRecognizeEmptyMessage);

        session.setAttribute("repeat_message",taskNameRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",taskNameRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(taskNameRecognizeEmptyMessage,cardTitle,taskNameRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getTaskNameEmptyResponse(Session session)
    {
        String taskNameEmptyMessage = "Sorry, i could not find your task name. " +
                "So please, first say the task name with the keyword task name. ";

        String cardTitle = "Task Name Empty Message";

        String taskNameEmptyRePromptMessage = "So please, first say the task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + taskNameEmptyMessage);

        session.setAttribute("repeat_message",taskNameEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",taskNameEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(taskNameEmptyMessage,cardTitle,taskNameEmptyRePromptMessage,true);
    }

    private SpeechletResponse getMoreTaskNameRecognizeEmptyResponse(String taskName, String paragraphText, Session session)
    {
        String moreTaskNameRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the more task name again with the keyword more task name. ";

        String cardTitle = "More Task Name Recognize Empty Message";

        String moreTaskNameRecognizeEmptyRePromptMessage = "Please say the more task name again with the keyword more task name. ";

        logger.debug(cardTitle + " : " + moreTaskNameRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("paragraph_text",paragraphText);
        session.setAttribute("repeat_message",moreTaskNameRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",moreTaskNameRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(moreTaskNameRecognizeEmptyMessage,cardTitle,moreTaskNameRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getBucketNameRecognizeEmptyResponse(String taskName, Session session)
    {
        String bucketNameRecognizeEmptyMessage = "Sorry, i could not find your bucket name. " +
                "So please, first say the bucket name with the keyword bucket name. ";

        String cardTitle = "Bucket Name Recognize Empty Message";

        String bucketNameRecognizeEmptyRePromptMessage = "So please, first say the bucket name with the keyword bucket name. ";

        logger.debug(cardTitle + " : " + bucketNameRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",bucketNameRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",bucketNameRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(bucketNameRecognizeEmptyMessage,cardTitle,bucketNameRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getBucketNameEmptyResponse(String taskName,Session session)
    {
        String bucketNameEmptyMessage = "Sorry, i could not find your bucket name. " +
                "So please, first say the bucket name with the keyword bucket name. ";

        String cardTitle = "Get Bucket Name Empty Message";

        String bucketNameEmptyRePromptMessage = "So please, first say the bucket name with the keyword bucket name. ";

        logger.debug(cardTitle + " : " + bucketNameEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",bucketNameEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",bucketNameEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(bucketNameEmptyMessage,cardTitle,bucketNameEmptyRePromptMessage,true);
    }

    private SpeechletResponse getFileNameRecognizeEmptyResponse(String taskName, String bucketName, Session session)
    {
        String bucketNameRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the file name again with the keyword file name. ";

        String cardTitle = "File Name Recognize Empty Message";

        String bucketNameRecognizeEmptyRePromptMessage = "Please say the file name again with keyword file name. ";

        logger.debug(cardTitle + " : " + bucketNameRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("repeat_message",bucketNameRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",bucketNameRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(bucketNameRecognizeEmptyMessage,cardTitle,bucketNameRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getFileNameEmptyResponse(String taskName, String bucketName, Session session)
    {
        String bucketNameEmptyMessage = "Sorry, i could not find your file name. " +
                "So please, first say the file name with the keyword file name. ";

        String cardTitle = "File Name Empty Message";

        String bucketNameEmptyRePromptMessage = "So please, first say the file name with the keyword file name. ";

        logger.debug(cardTitle + " : " + bucketNameEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("repeat_message",bucketNameEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",bucketNameEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(bucketNameEmptyMessage,cardTitle,bucketNameEmptyRePromptMessage,true);
    }

    private SpeechletResponse getFileFormatRecognizeEmptyResponse(String taskName, String bucketName, String fileName, Session session)
    {
        String fileFormatRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the file format again with the keyword file format. ";

        String cardTitle = "File Format Recognize Empty Message";

        String fileFormatRecognizeEmptyRePromptMessage = "Please say the file format again with keyword file format. ";

        logger.debug(cardTitle + " : " + fileFormatRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("file_name",fileName);
        session.setAttribute("repeat_message",fileFormatRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",fileFormatRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(fileFormatRecognizeEmptyMessage,cardTitle,fileFormatRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getFileFormatEmptyResponse(String taskName, String bucketName, String fileName, Session session)
    {
        String fileFormatEmptyMessage = "Sorry, i could not find your file format. " +
                "So please, first say the file format with the keyword file format. ";

        String cardTitle = "File Format Empty Message";

        String fileFormatEmptyRePromptMessage = "So please, first say the file format with the keyword file format. ";

        logger.debug(cardTitle + " : " + fileFormatEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("file_name",fileName);
        session.setAttribute("repeat_message",fileFormatEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",fileFormatEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(fileFormatEmptyMessage,cardTitle,fileFormatEmptyRePromptMessage,true);
    }

    private SpeechletResponse getTargetFileNameRecognizeEmptyResponse(String taskName, String bucketName, String fileName, String fileFormat, Session session)
    {
        String targetFileNameRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the target file name again with the keyword target file name. ";

        String cardTitle = "Target File Name Recognize Empty Message";

        String targetFileNameRecognizeEmptyRePromptMessage = "Please say the target file name again with keyword target file name. ";

        logger.debug(cardTitle + " : " + targetFileNameRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("file_name",fileName);
        session.setAttribute("file_format",fileFormat);
        session.setAttribute("repeat_message",targetFileNameRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",targetFileNameRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(targetFileNameRecognizeEmptyMessage,cardTitle,targetFileNameRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getTargetFileNameEmptyResponse(String taskName, String bucketName, String fileName, String fileFormat, Session session)
    {
        String targetFileNameRecognizeEmptyMessage = "Sorry, i could not find target your file name. " +
                "So please, first say the target file name with the keyword target file name. ";

        String cardTitle = "Target File Name Empty Message";

        String targetFileNameRecognizeEmptyRePromptMessage = "So please, first say the target file name with the keyword target file name. ";

        logger.debug(cardTitle + " : " + targetFileNameRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("file_name",fileName);
        session.setAttribute("file_format",fileFormat);
        session.setAttribute("repeat_message",targetFileNameRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",targetFileNameRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(targetFileNameRecognizeEmptyMessage,cardTitle,targetFileNameRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getTargetFileFormatRecognizeEmptyResponse(String taskName, String bucketName, String fileName, String fileFormat, String targetFileName, Session session)
    {
        String targetFileFormatRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the target file format again with the keyword target file format. ";

        String cardTitle = "Target File Format Recognize Empty Message";

        String targetFileFormatRecognizeEmptyRePromptMessage = "Please say the target file format again with keyword target file format. ";

        logger.debug(cardTitle + " : " + targetFileFormatRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("file_name",fileName);
        session.setAttribute("file_format",fileFormat);
        session.setAttribute("target_file_name",targetFileName);
        session.setAttribute("repeat_message",targetFileFormatRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",targetFileFormatRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(targetFileFormatRecognizeEmptyMessage,cardTitle,targetFileFormatRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getTargetFileFormatEmptyResponse(String taskName, String bucketName, String fileName, String fileFormat, String targetFileName, Session session)
    {
        String targetFileFormatEmptyMessage = "Sorry, i could not find your target file format. " +
                "So please, first say the target file format with the keyword target file format. ";

        String cardTitle = "Target File Format Empty Message";

        String targetFileFormatEmptyRePromptMessage = "So please, first say the source file format with the keyword target file format. ";

        logger.debug(cardTitle + " : " + targetFileFormatEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("bucket_name",bucketName);
        session.setAttribute("file_name",fileName);
        session.setAttribute("file_format",fileFormat);
        session.setAttribute("target_file_name",targetFileName);
        session.setAttribute("repeat_message",targetFileFormatEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",targetFileFormatEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(targetFileFormatEmptyMessage,cardTitle,targetFileFormatEmptyRePromptMessage,true);
    }

    private SpeechletResponse getParagraphTextRecognizeEmptyResponse(String taskName, Session session)
    {
        String paragraphTextRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the paragraph text again with the keyword paragraph text. ";

        String cardTitle = "Paragraph Text Recognize Empty Message";

        String paragraphTextRecognizeEmptyRePromptMessage = "Please say the paragraph text again with the keyword paragraph text. ";

        logger.debug(cardTitle + " : " + paragraphTextRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",paragraphTextRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",paragraphTextRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(paragraphTextRecognizeEmptyMessage,cardTitle,paragraphTextRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getParagraphTextEmptyResponse(String taskName, Session session)
    {
        String paragraphTextEmptyMessage = "Sorry, i could not find your paragraph text. " +
                "So please, first say the paragraph text with the keyword paragraph text. ";

        String cardTitle = "Paragraph Text Empty Message";

        String paragraphTextEmptyRePromptMessage =  "So please, first say the paragraph text with the keyword paragraph text. ";

        logger.debug(cardTitle + " : " + paragraphTextEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",paragraphTextEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",paragraphTextEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(paragraphTextEmptyMessage,cardTitle,paragraphTextEmptyRePromptMessage,true);
    }

    private SpeechletResponse getTargetLanguageRecognizeEmptyResponse(String taskName, String paragraphText, Session session)
    {
        String targetLanguageRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the valid target language again with the keyword target language. ";

        String cardTitle = "Target Language Recognize Empty Message";

        String targetLanguageRecognizeEmptyRePromptMessage = "Please say the valid target language again with the keyword target language. ";

        logger.debug(cardTitle + " : " + targetLanguageRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("paragraph_text",paragraphText);
        session.setAttribute("repeat_message",targetLanguageRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",targetLanguageRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(targetLanguageRecognizeEmptyMessage,cardTitle,targetLanguageRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getDefinitionWordRecognizeEmptyResponse(String taskName, Session session)
    {
        String definitionWordRecognizeEmptyMessage = "Sorry, i could not understand your voice. " +
                "Please say the definition word again with the keyword definition word. ";

        String cardTitle = "Definition Word Recognize Empty Message";

        String definitionWordRecognizeEmptyRePromptMessage = "Please say the definition word again with the keyword definition word. ";

        logger.debug(cardTitle + " : " + definitionWordRecognizeEmptyMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("repeat_message",definitionWordRecognizeEmptyMessage);
        session.setAttribute("repeat_re_prompt_message",definitionWordRecognizeEmptyRePromptMessage);

        return getMessageWithSimpleCardResponse(definitionWordRecognizeEmptyMessage,cardTitle,definitionWordRecognizeEmptyRePromptMessage,true);
    }

    private SpeechletResponse getWordDefinitionSuccessResponse(String word, String definition, Session session)
    {
        String wordDefinitionMessage = "Definition of " + word + " : " + definition + ". Okay, if you like to perform another task simply say the task name with keyword task name. ";

        String cardTitle = "Definition for " + word;

        String wordDefinitionRePromptMessage =  "Okay, if you like to perform another task simply say the task name with keyword task name. ";

        logger.debug(cardTitle + " : " + wordDefinitionMessage);

        session.removeAttribute("task_name");
        session.removeAttribute("paragraph_text");
        session.setAttribute("repeat_message",wordDefinitionMessage);
        session.setAttribute("repeat_re_prompt_message",wordDefinitionRePromptMessage);

        return getMessageWithSimpleCardResponse(wordDefinitionMessage,cardTitle,wordDefinitionRePromptMessage,true);
    }

    private SpeechletResponse getLikeToPerFormTaskResponse(String taskName, String paragraphText, Session session)
    {
        String iLikeToPerformMessage = "Okay, choose one task name from following one. " +
                "First one is text translate, second one is text key phrase detection and third one is text definition. " +
                "Okay, text translate is useful to translate the text paragraph. " +
                "And text key phrase detection is useful to detect key phrases from the text paragraph. " +
                "And finally text definition is useful to get definition for some word in the text paragraph. " +
                "Okay, now say any one task name from above one with the keyword more task name. ";

        String cardTitle = "I Like To Perform Message";

        String iLikeToPerformRePromptMessage = "Okay, now say any one task name from above one with the keyword more task name. ";

        logger.debug(cardTitle + " : " + iLikeToPerformMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("paragraph_text",paragraphText);
        session.setAttribute("repeat_message",iLikeToPerformMessage);
        session.setAttribute("repeat_re_prompt_message",iLikeToPerformRePromptMessage);

        return getMessageWithSimpleCardResponse(iLikeToPerformMessage,cardTitle,iLikeToPerformRePromptMessage,true);
    }

    private SpeechletResponse getWordDefinitionFailureResponse(String word, Session session)
    {
        SearchQuery wikiQuery = new SearchQuery.Builder(word).site("en.wikipedia.org").numResults(1).build();

        SearchResult wikiResult = new GoogleWebSearch().search(wikiQuery);

        ArrayList<String> urls = new ArrayList<>(wikiResult.getUrls());

        SearchQuery quoraQuery = new SearchQuery.Builder(word).site("quora.com").numResults(1).build();

        SearchResult quoraResult = new GoogleWebSearch().search(quoraQuery);

        urls.addAll(quoraResult.getUrls());

        SearchQuery stackQuery = new SearchQuery.Builder(word).site("stackoverflow.com").numResults(1).build();

        SearchResult stackResult = new GoogleWebSearch().search(stackQuery);

        urls.addAll(stackResult.getUrls());

        String listString = Joiner.on(", \n").join(urls);

        String wordDefinitionMessage = "I could not find the definition for " + word +
                ". Please go through the below best websites.\n" + listString +
                ". Okay, if you like to perform another task simply say the task name with keyword task name. ";

        String cardTitle = "Word Definition Message";

        String wordDefinitionRePromptMessage = "Okay, if you like to perform another task simply say the task name with keyword task name. ";

        logger.debug(cardTitle + " : " + wordDefinitionMessage);

        session.removeAttribute("task_name");
        session.removeAttribute("paragraph_text");
        session.setAttribute("repeat_message",wordDefinitionMessage);
        session.setAttribute("repeat_re_prompt_message",wordDefinitionRePromptMessage);

        return getMessageWithSimpleCardResponse(wordDefinitionMessage,cardTitle,wordDefinitionRePromptMessage,true);
    }

    private SpeechletResponse getTextTranslateResponse(String taskName, String paragraphText, Session session)
    {
        String getTextTranslateMessage = "Okay, now choose a target language from following one to translate the english text. " +
                "Arabic,Chinese,Chinese Traditional,Czech,Danish,Dutch,Finnish,French,German,Hebrew,Indonesian,Italian,Japanese,Korean,Polish,Portuguese,Russian,Spanish,Swedish,Turkish. " +
                "Okay, choose your target language from above one and say your target language with the keyword target language. ";

        String cardTitle = "Text Translate Message";

        String getTextTranslateRePromptMessage = "Okay, now choose a target language from following one to translate the english text. " +
                "Arabic,Chinese,Chinese Traditional,Czech,Danish,Dutch,Finnish,French,German,Hebrew,Indonesian,Italian,Japanese,Korean,Polish,Portuguese,Russian,Spanish,Swedish,Turkish. " +
                "Okay, choose your target language from above one and say your target language with the keyword target language. ";

        logger.debug(cardTitle + " : " + getTextTranslateMessage);

        session.setAttribute("task_name",taskName);
        session.setAttribute("paragraph_text",paragraphText);
        session.setAttribute("repeat_message",getTextTranslateMessage);
        session.setAttribute("repeat_re_prompt_message",getTextTranslateRePromptMessage);

        return getMessageWithSimpleCardResponse(getTextTranslateMessage,cardTitle,getTextTranslateRePromptMessage,true);
    }

    private SpeechletResponse getTextKeyPhraseDetectionResponse(String paragraphText, Session session)
    {
        AmazonComprehend comprehendClient = getAmazonComprehendClient();

        DetectKeyPhrasesRequest request = new DetectKeyPhrasesRequest()
                .withText(paragraphText)
                .withLanguageCode("en");

        DetectKeyPhrasesResult result = comprehendClient.detectKeyPhrases(request);

        if (result.getKeyPhrases().size() <= 0)
        {
            String textKeyPhraseDetectionMessage = "Okay, i could not detect any phrase from your paragraph text. " +
                    "So, please say a new paragraph. " +
                    "Or, if you like to perform another task simply say the task name with keyword task name. ";

            String cardTitle = "Text Key Phrase Detection Message";

            String textKeyPhraseDetectionRePromptMessage = "Or, if you like to perform another task simply say the task name with keyword task name. ";

            logger.debug(cardTitle + " : " + textKeyPhraseDetectionMessage);

            session.removeAttribute("task_name");
            session.removeAttribute("paragraph_text");
            session.setAttribute("repeat_message",textKeyPhraseDetectionMessage);
            session.setAttribute("repeat_re_prompt_message",textKeyPhraseDetectionRePromptMessage);

            return getMessageWithSimpleCardResponse(textKeyPhraseDetectionMessage,cardTitle,textKeyPhraseDetectionRePromptMessage,true);
        }
        else
        {
            StringBuilder stringBuilder = new StringBuilder();

            for (int i=0; i<result.getKeyPhrases().size(); i++)
            {
                if (i == result.getKeyPhrases().size() - 1)
                {
                    stringBuilder.append(i + 1).append(". ").append(result.getKeyPhrases().get(i).getText()).append(".");
                }
                else
                {
                    stringBuilder.append(i + 1).append(". ").append(result.getKeyPhrases().get(i).getText()).append(",");
                }
            }

            String textKeyPhraseDetectionMessage = "Okay, i detected some phrase from your paragraph text. " +
                    "Now i say the text phrases one by one, " +
                    stringBuilder.toString() +
                    "Okay this are detected phrases in your paragraph. " +
                    "If you like to perform another task simply say the task name with keyword task name. ";

            String cardTitle = "Text Key Phrase Detection Message";

            String textKeyPhraseDetectionRePromptMessage = "If you like to perform another task simply say the task name with keyword task name. ";

            logger.debug(cardTitle + " : " + textKeyPhraseDetectionMessage);

            session.removeAttribute("task_name");
            session.removeAttribute("paragraph_text");
            session.setAttribute("repeat_message",textKeyPhraseDetectionMessage);
            session.setAttribute("repeat_re_prompt_message",textKeyPhraseDetectionRePromptMessage);

            return getMessageWithSimpleCardResponse(textKeyPhraseDetectionMessage,cardTitle,textKeyPhraseDetectionRePromptMessage,true);
        }
    }

    private SpeechletResponse getFallbackResponse(Session session)
    {
        if (!getStoredSessionTaskName(session).isEmpty())
        {
            session.removeAttribute("task_name");
        }

        if (!getStoredSessionBucketName(session).isEmpty())
        {
            session.removeAttribute("bucket_name");
        }

        if (!getStoredSessionFileName(session).isEmpty())
        {
            session.removeAttribute("file_name");
        }

        if (!getStoredSessionFileFormat(session).isEmpty())
        {
            session.removeAttribute("file_format");
        }

        if (!getStoredSessionParagraphText(session).isEmpty())
        {
            session.removeAttribute("paragraph_text");
        }

        if (!getStoredSessionTargetFileName(session).isEmpty())
        {
            session.removeAttribute("target_file_name");
        }

        if (!getStoredSessionTargetFileFormat(session).isEmpty())
        {
            session.removeAttribute("target_file_format");
        }

        String fallbackMessage = "Sorry, some error has been occurred or some internal problem occurred. " +
                "Or, i could not understand your words. " +
                "So, please start your task freshly. " +
                "Or, if you want help say help. " +
                "Okay, now say a task name with the keyword task name. ";

        String cardTitle = "Fallback Message";

        String fallbackRePromptMessage = "Okay, now say a task name with the keyword task name. ";

        logger.debug(cardTitle + " : " + fallbackMessage);

        session.setAttribute("repeat_message",fallbackMessage);
        session.setAttribute("repeat_re_prompt_message",fallbackRePromptMessage);

        return getMessageWithSimpleCardResponse(fallbackMessage,cardTitle,fallbackRePromptMessage,true);
    }

    private SpeechletResponse getStopOrCancelResponse(Session session)
    {
        if (!getStoredSessionTaskName(session).isEmpty())
        {
            session.removeAttribute("task_name");
        }

        if (!getStoredSessionBucketName(session).isEmpty())
        {
            session.removeAttribute("bucket_name");
        }

        if (!getStoredSessionFileName(session).isEmpty())
        {
            session.removeAttribute("file_name");
        }

        if (!getStoredSessionFileFormat(session).isEmpty())
        {
            session.removeAttribute("file_format");
        }

        if (!getStoredSessionParagraphText(session).isEmpty())
        {
            session.removeAttribute("paragraph_text");
        }

        if (!getStoredSessionTargetFileName(session).isEmpty())
        {
            session.removeAttribute("target_file_name");
        }

        if (!getStoredSessionTargetFileFormat(session).isEmpty())
        {
            session.removeAttribute("target_file_format");
        }

        String stopOrCancelMessage = "Would you like to cancel or stop all the tasks and conversations?. " +
                "If yes say yes. " +
                "If no say no. ";

        String cardTitle = "Stop or Cancel Message";

        logger.debug(cardTitle + " : " + stopOrCancelMessage);

        session.setAttribute("repeat_message",stopOrCancelMessage);
        session.setAttribute("repeat_re_prompt_message",stopOrCancelMessage);

        return getMessageWithSimpleCardResponse(stopOrCancelMessage,cardTitle,stopOrCancelMessage,true);
    }

    private SpeechletResponse getYesResponse(Session session)
    {
        String yesMessage = "Ok, i stopped and terminated all the tasks and conversations. If you like to speak to me again. Simply you can say alexa, open solver pro.";

        String cardTitle = "Yes Message";

        logger.debug(cardTitle + " : " + yesMessage);

        session.setAttribute("repeat_message",yesMessage);
        session.setAttribute("repeat_re_prompt_message",yesMessage);

        return getMessageWithSimpleCardResponse(yesMessage,cardTitle,yesMessage,false);
    }

    private SpeechletResponse getNoResponse(Session session)
    {
        if (!getStoredSessionTaskName(session).isEmpty())
        {
            session.removeAttribute("task_name");
        }

        if (!getStoredSessionBucketName(session).isEmpty())
        {
            session.removeAttribute("bucket_name");
        }

        if (!getStoredSessionFileName(session).isEmpty())
        {
            session.removeAttribute("file_name");
        }

        if (!getStoredSessionFileFormat(session).isEmpty())
        {
            session.removeAttribute("file_format");
        }

        if (!getStoredSessionParagraphText(session).isEmpty())
        {
            session.removeAttribute("paragraph_text");
        }

        if (!getStoredSessionTargetFileName(session).isEmpty())
        {
            session.removeAttribute("target_file_name");
        }

        if (!getStoredSessionTargetFileFormat(session).isEmpty())
        {
            session.removeAttribute("target_file_format");
        }

        String noMessage = "Ok, don't worry. We can continue the conversation. " +
                "The all tasks has been terminated. " +
                "So, please say the task name with keyword task name. ";

        String cardTitle = "No Message";

        String noRePromptMessage = "So, please say the task name with keyword task name. ";

        logger.debug(cardTitle + " : " + noMessage);

        session.setAttribute("repeat_message",noMessage);
        session.setAttribute("repeat_re_prompt_message",noRePromptMessage);

        return getMessageWithSimpleCardResponse(noMessage,cardTitle,noRePromptMessage,true);
    }

    // Speechlet Simple Card Response

    private static SpeechletResponse getMessageWithSimpleCardResponse(String message, String cardTitle, String rePromptMessage, boolean askResponse)
    {
        SimpleCard simpleCard = new SimpleCard();
        simpleCard.setTitle(cardTitle);
        simpleCard.setContent(message);

        PlainTextOutputSpeech plainTextOutputSpeech = new PlainTextOutputSpeech();
        plainTextOutputSpeech.setText(message);

        if (askResponse)
        {
            PlainTextOutputSpeech rePromptOutputSpeech = new PlainTextOutputSpeech();
            rePromptOutputSpeech.setText(rePromptMessage);
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(rePromptOutputSpeech);

            return SpeechletResponse.newAskResponse(plainTextOutputSpeech,reprompt,simpleCard);
        }
        else
        {
            return SpeechletResponse.newTellResponse(plainTextOutputSpeech,simpleCard);
        }
    }
}
