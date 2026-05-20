using MediatR;

namespace ClimbBetter.Application.CQRS.Qualities.GetQualities;

public record GetQualitiesQuery : IRequest<List<QualityDto>>;