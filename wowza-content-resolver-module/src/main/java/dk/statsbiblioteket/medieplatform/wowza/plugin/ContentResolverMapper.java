package dk.statsbiblioteket.medieplatform.wowza.plugin;

import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamFileMapper;
import com.wowza.wms.stream.MediaStreamFileMapperBase;

import dk.statsbiblioteket.medieplatform.contentresolver.lib.ContentResolver;
import dk.statsbiblioteket.medieplatform.contentresolver.model.Resource;

import java.io.File;
import java.util.List;

/**
 * This class is used to let the user see the correct file
 */
public class ContentResolverMapper extends MediaStreamFileMapperBase implements IMediaStreamFileMapper {

    private static final File FILE_NOT_FOUND_NON_EXISTENT_FILE = new File("file_not_found");
    private final WMSLogger logger;
    private String presentationType;
    private final IMediaStreamFileMapper defaultMapper;
    private final ContentResolver contentResolver;

    public ContentResolverMapper(String presentationType, IMediaStreamFileMapper defaultMapper,
                                 ContentResolver contentResolver) {
        super();
        this.presentationType = presentationType;
        this.defaultMapper = defaultMapper;
        this.contentResolver = contentResolver;
        this.logger = WMSLoggerFactory.getLogger(this.getClass());
    }

    @Override
    public File streamToFileForRead(IMediaStream stream) {
        logger.trace("streamToFileForRead(IMediaStream stream=" + stream + ")");
        String name = stream.getName();
        String ext = stream.getExt();
        String query = stream.getQueryStr();
        return streamToFileForRead(stream, name, ext, query);
    }

    /**
     * This method is invoked when Wowza tries to figure out which file to play
     * @param stream the stream requested
     * @param name the name of the file to play
     * @param ext the extension of the file
     * @param streamQuery ?
     * @return the file to play
     */
    @Override
    public File streamToFileForRead(IMediaStream stream, String name, String ext, String streamQuery) {
        logger.trace(
                "streamToFileForRead(IMediaStream stream=" + stream + ", String name=" + name + ", String ext=" + ext
                        + ", String streamQuery=" + streamQuery + ")");
        IClient client = stream.getClient();
        if (client == null) {
            logger.debug("No client, returning ", stream);
            return null;
        }
        File streamingFile = getFileToStream(name);
        logger.debug(
                "streamToFileForRead(IMediaStream stream=" + stream + ", String name=" + name + ", String ext=" + ext
                        + ", String streamQuery=" + streamQuery + "). Resulting streaming file: '" + streamingFile
                        .getAbsolutePath() + "'");
        return streamingFile;
    }

    private String clean(String name) {
        if (name.contains(".")){
            name = name.substring(0, name.indexOf("."));
        }
        if (name.contains(":")) {
            name = name.substring(name.lastIndexOf(':') + 1);
        }

        return name;
    }

    /**
     * This method retrieves the filename from the ticket, by querying the content resolver to get the
     * streaming resource
     * @param name the filename
     * @return the file to stream
     */
    protected File getFileToStream(String name) {
        // Extract
        name = clean(name);
        logger.debug("Looking up '" + name + "'");
        List<Resource> resources = contentResolver.getContent(name).getResources();
        if (resources != null) {
            for (Resource resource : resources) {
                if (resource.getType().equals(presentationType)) {
                    String pathname = resource.getUris().get(0).getPath();
                    logger.info("Found '" + pathname + "' for '" + name + "'");
                    return new File(pathname);
                }
            }
        }
        logger.info("Content not found for: '" + name + "'");
        return FILE_NOT_FOUND_NON_EXISTENT_FILE;
    }

    @Override
    public File streamToFileForWrite(IMediaStream stream) {
        logger.trace("streamToFileForWrite(IMediaStream stream):" + stream);
        return defaultMapper.streamToFileForRead(stream);
    }

    @Override
    public File streamToFileForWrite(IMediaStream stream, String name, String ext, String query) {
        logger.trace("streamToFileForWrite(IMediaStream stream, String name, String ext, String query)" + stream);
        return defaultMapper.streamToFileForRead(stream, name, ext, query);
    }

}
