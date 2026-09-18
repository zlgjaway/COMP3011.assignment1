package comp3011.assignment1.service;

public class TranscriptionResponse {

    private String text;
    private Usage usage;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public static class Usage {

        private long input_tokens;
        private long output_tokens;

        public long getInput_tokens() {
            return input_tokens;
        }

        public void setInput_tokens(long input_tokens) {
            this.input_tokens = input_tokens;
        }

        public long getOutput_tokens() {
            return output_tokens;
        }

        public void setOutput_tokens(long output_tokens) {
            this.output_tokens = output_tokens;
        }
    }
}