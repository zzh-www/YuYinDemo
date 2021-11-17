class LayerNorm(Module):
  __parameters__ = ["weight", "bias", ]
  __buffers__ = []
  weight : Tensor
  bias : Tensor
  training : bool
  elementwise_affine : Final[bool] = True
  normalized_shape : Final[Tuple[int]] = (256,)
  eps : Final[float] = 9.9999999999999998e-13
  def forward(self: __torch__.torch.nn.modules.normalization.LayerNorm,
    input: Tensor) -> Tensor:
    _0 = __torch__.torch.nn.functional.layer_norm
    _1 = self.weight
    _2 = self.bias
    _3 = _0(input, [256], _1, _2, 9.9999999999999998e-13, )
    return _3
