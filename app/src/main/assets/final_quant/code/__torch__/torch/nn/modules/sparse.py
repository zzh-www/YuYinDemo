class Embedding(Module):
  __parameters__ = ["weight", ]
  __buffers__ = []
  weight : Tensor
  training : bool
  sparse : Final[bool] = False
  scale_grad_by_freq : Final[bool] = False
  embedding_dim : Final[int] = 256
  max_norm : Final[None] = None
  num_embeddings : Final[int] = 11008
  padding_idx : Final[None] = None
  norm_type : Final[float] = 2.
  def forward(self: __torch__.torch.nn.modules.sparse.Embedding,
    input: Tensor) -> Tensor:
    _0 = __torch__.torch.nn.functional.embedding
    _1 = _0(input, self.weight, None, None, 2., False, False, )
    return _1
