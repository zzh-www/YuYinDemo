class Sequential(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  __annotations__["0"] = __torch__.torch.nn.modules.sparse.Embedding
  __annotations__["1"] = __torch__.wenet.transformer.embedding.PositionalEncoding
  def forward(self: __torch__.torch.nn.modules.container.___torch_mangle_11.Sequential,
    input: Tensor) -> Tuple[Tensor, Tensor]:
    _0 = getattr(self, "0")
    _1 = getattr(self, "1")
    input0 = (_0).forward(input, )
    return (_1).forward(input0, 0, )
