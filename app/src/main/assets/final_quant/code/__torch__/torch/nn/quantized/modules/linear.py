class LinearPackedParams(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  dtype : int
  _packed_params : __torch__.torch.classes.quantized.LinearPackedParamsBase
  def forward(self: __torch__.torch.nn.quantized.modules.linear.LinearPackedParams,
    x: Tensor) -> Tensor:
    return x
  def __getstate__(self: __torch__.torch.nn.quantized.modules.linear.LinearPackedParams) -> Tuple[Tensor, Optional[Tensor], bool, int]:
    qweight, bias, = (self)._weight_bias()
    _0 = (qweight, bias, self.training, self.dtype)
    return _0
  def __setstate__(self: __torch__.torch.nn.quantized.modules.linear.LinearPackedParams,
    state: Tuple[Tensor, Optional[Tensor], bool, int]) -> None:
    self.dtype = (state)[3]
    _1 = (self).set_weight_bias((state)[0], (state)[1], )
    self.training = (state)[2]
    return None
  def _weight_bias(self: __torch__.torch.nn.quantized.modules.linear.LinearPackedParams) -> Tuple[Tensor, Optional[Tensor]]:
    _2 = uninitialized(Tuple[Tensor, Optional[Tensor]])
    if torch.eq(self.dtype, 12):
      _4, _5 = ops.quantized.linear_unpack(self._packed_params)
      _3 = (_4, _5)
    else:
      if torch.eq(self.dtype, 5):
        _7, _8 = ops.quantized.linear_unpack_fp16(self._packed_params)
        _6 = (_7, _8)
      else:
        ops.prim.RaiseException("Exception")
        _6 = _2
      _3 = _6
    return _3
  def set_weight_bias(self: __torch__.torch.nn.quantized.modules.linear.LinearPackedParams,
    weight: Tensor,
    bias: Optional[Tensor]) -> None:
    if torch.eq(self.dtype, 12):
      _9 = ops.quantized.linear_prepack(weight, bias)
      self._packed_params = _9
    else:
      if torch.eq(self.dtype, 5):
        _10 = ops.quantized.linear_prepack_fp16(weight, bias)
        self._packed_params = _10
      else:
        ops.prim.RaiseException("Exception")
    return None
