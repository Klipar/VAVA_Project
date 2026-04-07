from sentence_transformers import SentenceTransformer
import numpy as np

# Load the pre-trained SentenceTransformer model
model = SentenceTransformer('all-MiniLM-L6-v2')

# Function to get the embedding of a given text
def get_embedding(text: str) -> np.ndarray:
    embedding = model.encode(text, convert_to_numpy=True)
    return embedding

# Function to get embeddings for a batch of texts
def get_embeddings_batch(texts: list) -> np.ndarray:
    embeddings = model.encode(texts, convert_to_numpy=True)
    return embeddings