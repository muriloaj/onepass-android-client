package com.speechpro.onepass.core.rest.api;

import com.speechpro.onepass.core.exception.*;
import com.speechpro.onepass.core.rest.OnePassService;
import com.speechpro.onepass.core.rest.data.*;
import com.speechpro.onepass.core.sessions.PersonSession;
import com.speechpro.onepass.core.sessions.VerificationSession;
import com.speechpro.onepass.core.transport.ITransport;
import com.speechpro.onepass.core.utils.Converter;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;

/**
 * @author volobuev
 * @since 19.04.16
 */
public class RetroRestAPI implements ITransport {


    private final OnePassService service;

    public RetroRestAPI(String url) {

        Retrofit retrofit = new Retrofit.Builder().baseUrl(url)
                                                  .addConverterFactory(JacksonConverterFactory.create())
                                                  .build();

        service = retrofit.create(OnePassService.class);

    }

    @Override
    public PersonSession createPerson(String personId) throws CoreException {

        Response<Void> response = null;
        try {
            response = service.createPerson(Converter.convertPerson(personId)).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        processCode(response);
        return new PersonSession(this, personId, false);
    }

    @Override
    public PersonSession readPerson(String personId) throws CoreException {

        Response<Person> response = null;
        try {
            response = service.readPerson(personId).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        processCode(response);
        Person person = response.body();
        return person != null ? Converter.convertPerson(this, person) : null;
    }

    @Override
    public void deletePerson(String personId) throws CoreException {
        processResponse(service.deletePerson(personId));
    }

    @Override
    public void addVoiceSample(String personId,
                               byte[] voiceSample,
                               String passphrase,
                               int samplingRate) throws CoreException {
        processResponse(service.addVoiceSample(personId,
                                               new VoiceSample(voiceSample,
                                                               passphrase,
                                                               samplingRate)));
    }

    @Override
    public void addVoiceSample(String personId,
                               byte[] voiceSample,
                               String passphrase,
                               int gender,
                               int samplingRate) throws CoreException {
        processResponse(service.addVoiceSample(personId,
                                               new VoiceSample(voiceSample,
                                                               passphrase,
                                                               gender,
                                                               samplingRate)));
    }

    @Override
    public void addVoiceFeature(String personId, byte[] voiceFeature) throws CoreException {
        processResponse(service.addVoiceFeature(personId, new Data(voiceFeature)));
    }

    @Override
    public void deleteVoice(String personId) throws CoreException {
        processResponse(service.deleteVoice(personId));
    }

    @Override
    public void addFaceModel(String personId, byte[] faceModel) throws CoreException {
        processResponse(service.addFaceModel(personId, new FaceModel(new Data(faceModel))));
    }

    @Override
    public void addFaceSample(String personId, byte[] faceSample) throws CoreException {
        processResponse(service.addFaceSample(personId, new FaceSample(new Data(faceSample))));
    }

    @Override
    public void deleteFace(String personId) throws CoreException {
        processResponse(service.deleteFace(personId));
    }

    @Override
    public VerificationSession startVerificationSession(String personId) throws CoreException {

        Response<VerificationSessionResponse> response = null;
        try {
            response = service.startVerificationSession(personId).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        processCode(response);
        return Converter.convertVerification(this, response.body());
    }

    @Override
    public void addVerificationVoiceSample(VerificationSession session,
                                           byte[] voiceSample,
                                           int gender,
                                           int samplingRate) throws CoreException {
        String sessionId  = session.getUuid();
        String passphrase = session.getPassphrase();
        processResponse(service.addVerificationVoiceSample(sessionId,
                                                           new VoiceSample(voiceSample,
                                                                           passphrase,
                                                                           gender)));
    }

    @Override
    public void addVerificationVoiceFeature(VerificationSession session,
                                            byte[] voiceFeature) throws CoreException {
        processResponse(service.addVerificationVoiceFeature(session.getUuid(),new Data(voiceFeature)));
    }

    @Override
    public void addVerificationFaceSample(VerificationSession session, byte[] faceSample) throws CoreException {
        processResponse(service.addVerificationFaceSample(session.getUuid(), new FaceSample(new Data(faceSample))));
    }

    @Override
    public void addVerificationFaceModel(VerificationSession session, byte[] faceModel) throws CoreException {
        processResponse(service.addVerificationFaceModel(session.getUuid(), new FaceModel(new Data(faceModel))));
    }

    @Override
    public void addVerificationVideo(VerificationSession session, byte[] video) throws CoreException {
        processResponse(service.addVerificationVideo(session.getUuid(), new Video(video, session.getPassphrase())));
    }

    @Override
    public boolean liveness(VerificationSession session) throws CoreException {
        return false;
    }

    @Override
    public boolean verify(VerificationSession session, boolean closeFlag) throws CoreException {
        Response<VerificationResult> response = null;
        try {
            response = service.verify(session.getUuid(), closeFlag).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        processCode(response);
        return Converter.convertVerificationResult(response.body());
    }

    @Override
    public void closeVerification(VerificationSession session) throws CoreException {
        processResponse(service.closeVerification(session.getUuid()));
    }

    private <T> void processResponse(Call<T> call) throws CoreException {
        try {
            Response<T> response = call.execute();
            processCode(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processCode(Response response) throws CoreException {
        if (response == null) {
            throw new ServiceUnavailableException("OnePass service is unavailable");
        }
        int    code    = response.code();
        String message = response.raw().message();
        switch (code) {
            case 400:
                throw new BadRequestException(message);
            case 403:
                throw new ForbiddenException(message);
            case 404:
                throw new NotFoundException(message);
            case 500:
                throw new InternalServerException(message);
        }
    }

}