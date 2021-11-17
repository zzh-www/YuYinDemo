class Conv1d(Module):
  __parameters__ = ["weight", "bias", ]
  __buffers__ = []
  weight : Tensor
  bias : Optional[Tensor]
  training : bool
  transposed : bool
  _reversed_padding_repeated_twice : Tuple[int, int]
  out_channels : Final[int] = 256
  kernel_size : Final[Tuple[int]] = (15,)
  in_channels : Final[int] = 256
  output_padding : Final[Tuple[int]] = (0,)
  dilation : Final[Tuple[int]] = (1,)
  stride : Final[Tuple[int]] = (1,)
  padding : Final[Tuple[int]] = (0,)
  groups : Final[int] = 256
  padding_mode : Final[str] = "zeros"
  def forward(self: __torch__.torch.nn.modules.conv.___torch_mangle_7.Conv1d,
    input: Tensor) -> Tensor:
    _0 = torch.conv1d(input, self.weight, self.bias, [1], [0], [1], 256)
    return _0
