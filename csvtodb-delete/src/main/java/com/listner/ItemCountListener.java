package com.listner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
 
public class ItemCountListener implements ChunkListener {
     
	private static final Logger log = LoggerFactory.getLogger(ItemCountListener.class);
    @Override
    public void beforeChunk(ChunkContext context) {
    }
 
    @Override
    public void afterChunk(ChunkContext context) {
         
        int count = context.getStepContext().getStepExecution().getReadCount();
        log.info("ItemCount: {}", count);
    }
     
    @Override
    public void afterChunkError(ChunkContext context) {
    }
}